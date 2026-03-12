import Foundation

/// Native Swift formatting helpers that mirror FormatUtils in the shared KMP module.
/// iOS DesignSystem components must NOT import FormatUtils (KMP-only).
enum FormatHelper {
    /// Formats a count with K/M suffixes (e.g. 1500 -> "1.5K", 2000000 -> "2.0M").
    static func formatCount(_ count: Int) -> String {
        if count >= 1_000_000 {
            return String(format: "%.1fM", Double(count) / 1_000_000)
        } else if count >= 1_000 {
            return String(format: "%.1fK", Double(count) / 1_000)
        }
        return "\(count)"
    }

    /// Formats a rating to one decimal place (e.g. 4.567 -> "4.6").
    static func formatRating(_ rating: Double) -> String {
        String(format: "%.1f", rating)
    }

    /// Formats a file size in KB to human-readable string (e.g. 1500.0 -> "1.5 MB").
    static func formatFileSize(sizeKB: Double) -> String {
        if sizeKB >= 1_000_000 {
            return String(format: "%.1f GB", sizeKB / 1_000_000)
        } else if sizeKB >= 1_000 {
            return String(format: "%.1f MB", sizeKB / 1_000)
        }
        return "\(Int(sizeKB.rounded())) KB"
    }
}
