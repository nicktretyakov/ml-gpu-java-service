// Library file for shared code and modules

// Re-export ml module to be used by both client and server
pub mod ml {
    tonic::include_proto!("ml");
}

// Include the GPU compute module
pub mod gpu_compute;

// Include the matrix operations module
pub mod matrix_ops;