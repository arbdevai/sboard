//! Atomic Modifier State Machine (Shift, Caps, Ctrl, Fn)

use std::sync::atomic::{AtomicU32, Ordering};

pub const MOD_SHIFT: u32 = 1 << 0;
pub const MOD_CAPS_LOCK: u32 = 1 << 1;
pub const MOD_CTRL: u32 = 1 << 2;
pub const MOD_FN: u32 = 1 << 3;
pub const MOD_ALT: u32 = 1 << 4;

pub struct ModifierStateMachine {
    state: AtomicU32,
}

impl ModifierStateMachine {
    pub const fn new() -> Self {
        Self {
            state: AtomicU32::new(0),
        }
    }

    #[inline]
    pub fn get(&self) -> u32 {
        self.state.load(Ordering::Relaxed)
    }

    #[inline]
    pub fn toggle(&self, flag: u32) -> u32 {
        self.state.fetch_xor(flag, Ordering::Relaxed) ^ flag
    }

    #[inline]
    pub fn set(&self, flag: u32, active: bool) {
        if active {
            self.state.fetch_or(flag, Ordering::Relaxed);
        } else {
            self.state.fetch_and(!flag, Ordering::Relaxed);
        }
    }

    #[inline]
    pub fn is_active(&self, flag: u32) -> bool {
        (self.get() & flag) != 0
    }
}
