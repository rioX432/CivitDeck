import AVKit
import SwiftUI

struct VideoPlayerView: View {
    let url: URL
    var autoPlay: Bool = true

    @StateObject private var playerHolder = AVPlayerHolder()

    var body: some View {
        VideoPlayer(player: playerHolder.player)
            .onAppear {
                playerHolder.setup(url: url, autoPlay: autoPlay)
            }
            .onDisappear {
                playerHolder.pause()
            }
    }
}

private class AVPlayerHolder: ObservableObject {
    @Published var player: AVPlayer?
    private var looper: AVPlayerLooper?
    private var queuePlayer: AVQueuePlayer?

    func setup(url: URL, autoPlay: Bool) {
        guard player == nil else { return }
        let item = AVPlayerItem(url: url)
        let queue = AVQueuePlayer(playerItem: item)
        let playerLooper = AVPlayerLooper(player: queue, templateItem: item)

        self.queuePlayer = queue
        self.looper = playerLooper
        self.player = queue

        if autoPlay {
            queue.play()
        }
    }

    func pause() {
        player?.pause()
    }

    deinit {
        player?.pause()
        player = nil
        looper = nil
    }
}
