import Foundation
import Shared

// MARK: - PromptTemplateEngine
// Swift mirror of com.riox432.civitdeck.domain.export.PromptTemplateEngine

final class PromptTemplateEngine {
    static let shared = PromptTemplateEngine()

    private let pattern: NSRegularExpression = {
        guard let regex = try? NSRegularExpression(pattern: "\\{([^{}]+)\\}") else {
            fatalError("Invalid regex pattern in SharedServices")
        }
        return regex
    }()
    private let openPlaceholder = "\u{0000}OPEN\u{0000}"
    private let closePlaceholder = "\u{0000}CLOSE\u{0000}"

    func extractVariables(template: String) -> [String] {
        let escaped = preEscape(template)
        let range = NSRange(escaped.startIndex..., in: escaped)
        var seen = Set<String>()
        var result = [String]()
        for match in pattern.matches(in: escaped, range: range) {
            if let r = Range(match.range(at: 1), in: escaped) {
                let name = String(escaped[r]).trimmingCharacters(in: .whitespaces)
                if seen.insert(name).inserted { result.append(name) }
            }
        }
        return result
    }

    func substitute(template: String, values: [String: String]) -> String {
        var text = preEscape(template)
        let range = NSRange(text.startIndex..., in: text)
        let matches = pattern.matches(in: text, range: range).reversed()
        for match in matches {
            guard let fullRange = Range(match.range, in: text),
                  let keyRange = Range(match.range(at: 1), in: text) else { continue }
            let key = String(text[keyRange]).trimmingCharacters(in: .whitespaces)
            let replacement = values[key] ?? String(text[fullRange])
            text.replaceSubrange(fullRange, with: replacement)
        }
        return postUnescape(text)
    }

    private func preEscape(_ s: String) -> String {
        s.replacingOccurrences(of: "{{", with: openPlaceholder)
         .replacingOccurrences(of: "}}", with: closePlaceholder)
    }

    private func postUnescape(_ s: String) -> String {
        s.replacingOccurrences(of: openPlaceholder, with: "{")
         .replacingOccurrences(of: closePlaceholder, with: "}")
    }
}

// MARK: - WorkflowExportService
// Swift mirror of com.riox432.civitdeck.domain.export.WorkflowExportService

final class WorkflowExportService {
    static let shared = WorkflowExportService()

    func generateA1111Params(meta: ImageGenerationMeta) -> String {
        var parts = [String]()
        if let prompt = meta.prompt { parts.append(prompt) }
        if let neg = meta.negativePrompt { parts.append("Negative prompt: \(neg)") }
        var params = [String]()
        if let steps = meta.steps { params.append("Steps: \(steps.intValue)") }
        if let sampler = meta.sampler { params.append("Sampler: \(sampler)") }
        if let cfg = meta.cfgScale { params.append("CFG scale: \(cfg.doubleValue)") }
        if let seed = meta.seed { params.append("Seed: \(seed.int64Value)") }
        if let size = meta.size { params.append("Size: \(size)") }
        if let model = meta.model { params.append("Model: \(model)") }
        if let hash = meta.additionalParams["Model hash"] { params.append("Model hash: \(hash)") }
        if !params.isEmpty { parts.append(params.joined(separator: ", ")) }
        return parts.joined(separator: "\n")
    }

    func generateComfyUIWorkflow(meta: ImageGenerationMeta) -> String {
        let (width, height) = parseSize(meta.size)
        let (samplerName, scheduler) = parseSampler(meta.sampler)
        let workflow: [String: Any] = [
            "3": checkpointLoaderNode(meta.model),
            "6": clipTextEncodeNode(meta.prompt ?? ""),
            "7": clipTextEncodeNode(meta.negativePrompt ?? ""),
            "5": emptyLatentImageNode(width, height),
            "4": kSamplerNode(meta, samplerName, scheduler),
            "8": ["class_type": "VAEDecode",
                  "inputs": ["samples": ["4", 0] as [Any], "vae": ["3", 2] as [Any]]
                  ] as [String: Any],
            "9": ["class_type": "SaveImage",
                  "inputs": ["filename_prefix": "CivitDeck", "images": ["8", 0] as [Any]]
                  ] as [String: Any],
        ]
        guard let data = try? JSONSerialization.data(withJSONObject: workflow, options: .prettyPrinted),
              let text = String(data: data, encoding: .utf8) else { return "{}" }
        return text
    }

    private func checkpointLoaderNode(_ model: String?) -> [String: Any] {
        ["class_type": "CheckpointLoaderSimple",
         "inputs": ["ckpt_name": model ?? "model.safetensors"]]
    }

    private func clipTextEncodeNode(_ text: String) -> [String: Any] {
        ["class_type": "CLIPTextEncode",
         "inputs": ["text": text, "clip": ["3", 1] as [Any]]]
    }

    private func emptyLatentImageNode(_ w: Int, _ h: Int) -> [String: Any] {
        ["class_type": "EmptyLatentImage",
         "inputs": ["width": w, "height": h, "batch_size": 1]]
    }

    private func kSamplerNode(_ meta: ImageGenerationMeta,
                              _ samplerName: String,
                              _ scheduler: String) -> [String: Any] {
        ["class_type": "KSampler",
         "inputs": [
            "seed": meta.seed?.int64Value ?? 0,
            "steps": meta.steps?.intValue ?? 20,
            "cfg": meta.cfgScale?.doubleValue ?? 7.0,
            "sampler_name": samplerName,
            "scheduler": scheduler,
            "denoise": 1.0,
            "model": ["3", 0] as [Any],
            "positive": ["6", 0] as [Any],
            "negative": ["7", 0] as [Any],
            "latent_image": ["5", 0] as [Any],
         ] as [String: Any]]
    }

    private func parseSize(_ size: String?) -> (Int, Int) {
        guard let size else { return (512, 512) }
        let parts = size.split(separator: "x").map { $0.trimmingCharacters(in: .whitespaces) }
        guard parts.count == 2, let w = Int(parts[0]), let h = Int(parts[1]) else { return (512, 512) }
        return (w, h)
    }

    private let samplerMap: [String: String] = [
        "euler_a": "euler_ancestral", "euler_ancestral": "euler_ancestral",
        "euler": "euler", "dpm++_2m": "dpmpp_2m", "dpm++_2s_a": "dpmpp_2s_ancestral",
        "dpm++_sde": "dpmpp_sde", "dpm++_2m_sde": "dpmpp_2m_sde",
        "dpm++_3m_sde": "dpmpp_3m_sde", "ddim": "ddim", "lms": "lms",
        "heun": "heun", "uni_pc": "uni_pc", "dpm_2": "dpm_2", "dpm_2_a": "dpm_2_ancestral",
    ]

    private func parseSampler(_ sampler: String?) -> (String, String) {
        guard let sampler else { return ("euler", "normal") }
        let normalized = sampler.lowercased().trimmingCharacters(in: .whitespaces)
        let isKarras = normalized.contains("karras")
        let cleaned = normalized.replacingOccurrences(of: "karras", with: "")
            .trimmingCharacters(in: .whitespaces).replacingOccurrences(of: " ", with: "_")
        return (samplerMap[cleaned] ?? cleaned, isKarras ? "karras" : "normal")
    }
}
