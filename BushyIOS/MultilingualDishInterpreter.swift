import Foundation

public enum LanguageScript: String, CaseIterable {
    case devanagari = "Devanagari (Hindi / Marathi)"
    case bengali = "Bengali (Bengali / Assamese)"
    case tamil = "Tamil"
    case telugu = "Telugu"
    case gujarati = "Gujarati"
    case kannada = "Kannada"
    case malayalam = "Malayalam"
    case gurmukhi = "Gurmukhi (Punjabi)"
    case latinEnglish = "Latin / English"
    
    public static func detectScript(text: String) -> LanguageScript {
        for scalar in text.unicodeScalars {
            let v = scalar.value
            switch v {
            case 0x0900...0x097F: return .devanagari
            case 0x0980...0x09FF: return .bengali
            case 0x0B80...0x0BFF: return .tamil
            case 0x0C00...0x0C7F: return .telugu
            case 0x0A80...0x0AFF: return .gujarati
            case 0x0C80...0x0CFF: return .kannada
            case 0x0D00...0x0D7F: return .malayalam
            case 0x0A00...0x0A7F: return .gurmukhi
            default: continue
            }
        }
        return .latinEnglish
    }
}

public struct DishInterpretationResult {
    public var originalInput: String
    public var normalizedName: String
    public var script: LanguageScript
    public var confidenceScore: Double // 0.0 to 1.0
    public var bestMatch: IndianNutritionEntry?
    public var candidateMatches: [IndianNutritionEntry]
    public var suggestedCorrection: String?
    public var isAmbiguous: Bool
}

public struct MultilingualDishInterpreter {
    
    public static func interpretDishText(_ rawInput: String) -> DishInterpretationResult {
        let trimmed = rawInput.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else {
            let fallback = IndianNutritionDatabase.items.first!
            return DishInterpretationResult(
                originalInput: rawInput,
                normalizedName: fallback.name,
                script: .latinEnglish,
                confidenceScore: 0.0,
                bestMatch: fallback,
                candidateMatches: [],
                suggestedCorrection: nil,
                isAmbiguous: false
            )
        }
        
        let script = LanguageScript.detectScript(text: trimmed)
        let normalizedInput = trimmed.lowercased()
            .components(separatedBy: CharacterSet.alphanumerics.inverted)
            .filter { !$0.isEmpty }
            .joined(separator: " ")
        
        let allItems = IndianNutritionDatabase.items
        
        // 1. Exact alias match
        let directMatches = allItems.filter { entry in
            entry.name.lowercased() == normalizedInput ||
            entry.aliases.contains { $0.lowercased() == normalizedInput }
        }
        
        if let best = directMatches.first {
            return DishInterpretationResult(
                originalInput: rawInput,
                normalizedName: best.name,
                script: script,
                confidenceScore: 1.0,
                bestMatch: best,
                candidateMatches: directMatches,
                suggestedCorrection: nil,
                isAmbiguous: directMatches.count > 2
            )
        }
        
        // 2. Substring & Partial match
        let partialMatches = allItems.filter { entry in
            entry.name.lowercased().contains(normalizedInput) ||
            entry.aliases.contains { $0.lowercased().contains(normalizedInput) || normalizedInput.contains($0.lowercased()) }
        }
        
        let genericTerms = ["dal", "daal", "दाल", "paneer", "पनीर", "rice", "चावल", "curry", "dosa", "डोसा", "paratha", "पराठा"]
        let isGeneric = genericTerms.contains(normalizedInput)
        
        if let best = partialMatches.first {
            let correction = (best.name.lowercased() != normalizedInput) ? best.name : nil
            return DishInterpretationResult(
                originalInput: rawInput,
                normalizedName: best.name,
                script: script,
                confidenceScore: isGeneric ? 0.65 : 0.88,
                bestMatch: best,
                candidateMatches: partialMatches,
                suggestedCorrection: correction,
                isAmbiguous: isGeneric || partialMatches.count > 1
            )
        }
        
        // 3. Fuzzy Levenshtein Distance
        var bestFuzzyEntry: IndianNutritionEntry? = nil
        var minDistance = Int.max
        
        for entry in allItems {
            let aliasList = [entry.name] + entry.aliases
            for alias in aliasList {
                let dist = levenshtein(normalizedInput, alias.lowercased())
                if dist < minDistance && dist <= 3 {
                    minDistance = dist
                    bestFuzzyEntry = entry
                }
            }
        }
        
        if let best = bestFuzzyEntry, minDistance <= 3 {
            let lengthRatio = Double(minDistance) / Double(max(normalizedInput.count, 1))
            let conf = min(max(1.0 - lengthRatio, 0.5), 0.9)
            return DishInterpretationResult(
                originalInput: rawInput,
                normalizedName: best.name,
                script: script,
                confidenceScore: conf,
                bestMatch: best,
                candidateMatches: [best],
                suggestedCorrection: best.name,
                isAmbiguous: false
            )
        }
        
        // 4. Fallback default
        return DishInterpretationResult(
            originalInput: rawInput,
            normalizedName: trimmed,
            script: script,
            confidenceScore: 0.0,
            bestMatch: nil,
            candidateMatches: [],
            suggestedCorrection: nil,
            isAmbiguous: false
        )
    }
    
    private static func levenshtein(_ s1: String, _ s2: String) -> Int {
        let a = Array(s1)
        let b = Array(s2)
        var dist = [[Int]](repeating: [Int](repeating: 0, count: b.count + 1), count: a.count + 1)
        
        for i in 0...a.count { dist[i][0] = i }
        for j in 0...b.count { dist[0][j] = j }
        
        for i in 1...a.count {
            for j in 1...b.count {
                if a[i - 1] == b[j - 1] {
                    dist[i][j] = dist[i - 1][j - 1]
                } else {
                    dist[i][j] = min(dist[i - 1][j] + 1, min(dist[i][j - 1] + 1, dist[i - 1][j - 1] + 1))
                }
            }
        }
        return dist[a.count][b.count]
    }
}
