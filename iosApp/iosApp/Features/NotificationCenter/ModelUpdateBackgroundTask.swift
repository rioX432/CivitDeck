import BackgroundTasks
import UserNotifications
import Shared

enum ModelUpdateBackgroundTask {
    static let taskIdentifier = "com.riox432.civitdeck.modelUpdateCheck"

    static func register() {
        BGTaskScheduler.shared.register(
            forTaskWithIdentifier: taskIdentifier,
            using: nil
        ) { task in
            guard let bgTask = task as? BGAppRefreshTask else { return }
            handleTask(bgTask)
        }
    }

    static func schedule() {
        let request = BGAppRefreshTaskRequest(identifier: taskIdentifier)
        request.earliestBeginDate = Date(timeIntervalSinceNow: 60 * 60)
        do {
            try BGTaskScheduler.shared.submit(request)
        } catch {
            // Background tasks may not be available in simulator
        }
    }

    private static func handleTask(_ task: BGAppRefreshTask) {
        schedule()

        let workTask = Task {
            guard await isUpdateCheckEnabled() else {
                // SKIE ends a cancelled for-await loop without values, so an expired task lands here too.
                task.setTaskCompleted(success: !Task.isCancelled)
                return
            }
            do {
                let useCase = KoinHelper.shared.getCheckAndStoreModelUpdatesUseCase()
                let updates = try await useCase.invoke()
                let updateList = updates.compactMap { $0 as? ModelUpdate }

                if !updateList.isEmpty {
                    await sendLocalNotification(updates: updateList)
                }
                task.setTaskCompleted(success: true)
            } catch {
                task.setTaskCompleted(success: false)
            }
        }

        task.expirationHandler = {
            workTask.cancel()
        }
    }

    // The task stays scheduled at every launch, so the user's settings gate the network call
    // here, mirroring Android scheduling its worker only when alerts are on and the interval is not Off.
    private static func isUpdateCheckEnabled() async -> Bool {
        var alertsEnabled = false
        for await value in KoinHelper.shared.getObserveNotificationsEnabledUseCase().invoke() {
            alertsEnabled = value.boolValue
            break
        }
        guard alertsEnabled else { return false }

        var interval = PollingInterval.off
        for await value in KoinHelper.shared.getObservePollingIntervalUseCase().invoke() {
            interval = value
            break
        }
        return interval != .off
    }

    private static func sendLocalNotification(updates: [ModelUpdate]) async {
        let center = UNUserNotificationCenter.current()
        let settings = await center.notificationSettings()
        guard settings.authorizationStatus == .authorized else { return }

        let content = UNMutableNotificationContent()
        if updates.count == 1, let first = updates.first {
            content.title = "\(first.modelName) updated"
            content.body = "New version: \(first.newVersionName)"
        } else {
            content.title = "\(updates.count) models updated"
            content.body = updates.map { "\($0.modelName): \($0.newVersionName)" }
                .joined(separator: "\n")
        }
        content.sound = .default

        let request = UNNotificationRequest(
            identifier: "model-update-\(Date().timeIntervalSince1970)",
            content: content,
            trigger: nil
        )
        try? await center.add(request)
    }
}
