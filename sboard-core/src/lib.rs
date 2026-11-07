//! Sboard Core: Native JNI Bridge for Android

pub mod bigram;
pub mod spatial;
pub mod state;

use jni::objects::{JClass, JFloatArray};
use jni::sys::{jchar, jfloat, jint, jstring};
use jni::JNIEnv;

use spatial::KeyCenter;
use state::ModifierStateMachine;

static STATE_MACHINE: ModifierStateMachine = ModifierStateMachine::new();

#[no_mangle]
pub extern "system" fn Java_com_sahalos_sboard_core_NativeCoreBridge_nativeGetVersion<'local>(
    env: JNIEnv<'local>,
    _class: JClass<'local>,
) -> jstring {
    let output = env
        .new_string("sboard-core-rust v0.2.0 (Spatial Hitbox & Bigram Probability)")
        .expect("Couldn't create java string");
    output.into_raw()
}

#[no_mangle]
pub extern "system" fn Java_com_sahalos_sboard_core_NativeCoreBridge_nativeGetBigramScore<'local>(
    _env: JNIEnv<'local>,
    _class: JClass<'local>,
    prev_char: jchar,
    next_char: jchar,
) -> jfloat {
    let prev = char::from_u32(prev_char as u32).unwrap_or(' ');
    let next = char::from_u32(next_char as u32).unwrap_or(' ');
    bigram::get_bigram_probability(prev, next)
}

#[no_mangle]
pub extern "system" fn Java_com_sahalos_sboard_core_NativeCoreBridge_nativeResolveHit<'local>(
    env: JNIEnv<'local>,
    _class: JClass<'local>,
    touch_x: jfloat,
    touch_y: jfloat,
    prev_char: jchar,
    flat_key_data: JFloatArray<'local>,
) -> jint {
    // Each key has 5 float values: [id, center_x, center_y, width, height]
    let len = match env.get_array_length(&flat_key_data) {
        Ok(l) => l as usize,
        Err(_) => return -1,
    };

    if len < 5 || len % 5 != 0 {
        return -1;
    }

    let mut buf = vec![0.0f32; len];
    if env.get_float_array_region(&flat_key_data, 0, &mut buf).is_err() {
        return -1;
    }

    let mut keys = Vec::with_capacity(len / 5);
    for chunk in buf.chunks_exact(5) {
        keys.push(KeyCenter {
            key_id: chunk[0] as i32,
            center_x: chunk[1],
            center_y: chunk[2],
            width: chunk[3],
            height: chunk[4],
        });
    }

    let prev = char::from_u32(prev_char as u32).unwrap_or(' ');
    let has_prev_alpha = prev.is_ascii_alphabetic();

    // Score combined with spatial Gaussian + Bigram transition probability
    let sigma = 26.0f32;
    let mut best_id = -1;
    let mut best_score = -1.0f32;

    for key in &keys {
        let spatial_prob = key.probability(touch_x, touch_y, sigma);
        let mut combined_score = spatial_prob;

        if has_prev_alpha && ((key.key_id >= 65 && key.key_id <= 90) || (key.key_id >= 97 && key.key_id <= 122)) {
            let candidate_char = char::from_u32(key.key_id as u32).unwrap_or(' ');
            let bigram_prob = bigram::get_bigram_probability(prev, candidate_char);
            combined_score = spatial_prob * (0.72f32 + 0.28f32 * bigram_prob);
        }

        if combined_score > best_score {
            best_score = combined_score;
            best_id = key.key_id;
        }
    }

    best_id
}

#[no_mangle]
pub extern "system" fn Java_com_sahalos_sboard_core_NativeCoreBridge_nativeToggleModifier<'local>(
    _env: JNIEnv<'local>,
    _class: JClass<'local>,
    modifier_flag: jint,
) -> jint {
    STATE_MACHINE.toggle(modifier_flag as u32) as jint
}

#[no_mangle]
pub extern "system" fn Java_com_sahalos_sboard_core_NativeCoreBridge_nativeIsModifierActive<'local>(
    _env: JNIEnv<'local>,
    _class: JClass<'local>,
    modifier_flag: jint,
) -> jint {
    if STATE_MACHINE.is_active(modifier_flag as u32) {
        1
    } else {
        0
    }
}

#[no_mangle]
pub extern "system" fn Java_com_sahalos_sboard_core_NativeCoreBridge_nativeResetModifiers<'local>(
    _env: JNIEnv<'local>,
    _class: JClass<'local>,
) {
    STATE_MACHINE.reset();
}
