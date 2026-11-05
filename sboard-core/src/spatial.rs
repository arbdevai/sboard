//! Spatial Hitbox Engine: Euclidean Distance & Gaussian Probability

#[derive(Debug, Clone, Copy)]
pub struct KeyCenter {
    pub key_id: i32,
    pub center_x: f32,
    pub center_y: f32,
    pub width: f32,
    pub height: f32,
}

impl KeyCenter {
    #[inline]
    pub fn distance_squared(&self, touch_x: f32, touch_y: f32) -> f32 {
        let dx = touch_x - self.center_x;
        let dy = touch_y - self.center_y;
        (dx * dx) + (dy * dy)
    }

    #[inline]
    pub fn distance(&self, touch_x: f32, touch_y: f32) -> f32 {
        self.distance_squared(touch_x, touch_y).sqrt()
    }

    /// Computes spatial Gaussian probability: P(key | touch) = exp(-d^2 / (2 * sigma^2))
    #[inline]
    pub fn probability(&self, touch_x: f32, touch_y: f32, sigma: f32) -> f32 {
        let d2 = self.distance_squared(touch_x, touch_y);
        let denom = 2.0 * sigma * sigma;
        (-d2 / denom).exp()
    }
}

/// Finds the best matching key from an array of key centers given touch coordinates
pub fn find_nearest_key(touch_x: f32, touch_y: f32, keys: &[KeyCenter], sigma: f32) -> Option<i32> {
    if keys.is_empty() {
        return None;
    }

    let mut best_key = keys[0].key_id;
    let mut best_score = -1.0f32;

    for key in keys {
        let prob = key.probability(touch_x, touch_y, sigma);
        if prob > best_score {
            best_score = prob;
            best_key = key.key_id;
        }
    }

    Some(best_key)
}
