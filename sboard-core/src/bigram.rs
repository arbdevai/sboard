//! Bigram Language & Anti-Typo Probability Model

/// Fast table lookup for common character transitions in Indonesian and English
pub fn get_bigram_probability(prev: char, current: char) -> f32 {
    let p = prev.to_ascii_lowercase();
    let c = current.to_ascii_lowercase();

    // High frequency digraphs in Indonesian & English
    match (p, c) {
        // Indonesian high frequency: ng, ny, me, di, se, ka, ke, an, da, te, be, pe
        ('n', 'g') | ('n', 'y') => 0.95,
        ('m', 'e') | ('d', 'i') | ('s', 'e') => 0.90,
        ('k', 'e') | ('k', 'a') | ('d', 'a') => 0.88,
        ('a', 'n') | ('i', 'n') | ('t', 'e') => 0.85,
        ('b', 'e') | ('p', 'e') | ('t', 'a') => 0.82,
        ('u', 'k') | ('a', 'k') | ('i', 'k') => 0.80,

        // English high frequency: th, he, in, er, an, re, on, at, en, nd, ti, es, or
        ('t', 'h') | ('h', 'e') | ('e', 'r') => 0.92,
        ('r', 'e') | ('o', 'n') | ('a', 't') => 0.86,
        ('n', 'd') | ('t', 'i') | ('e', 's') => 0.84,

        // Unlikely / impossible combinations (vowel cluster protection)
        ('q', 'u') => 0.98,
        ('q', _) => 0.05,

        _ => 0.50, // Default neutral prior
    }
}
