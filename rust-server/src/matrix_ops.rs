// Matrix operations module for ML Hybrid System

use crate::ml::{Matrix, MatrixComputeRequest, MatrixComputeResponse};
use std::time::Instant;

// Error type for matrix operations
#[derive(Debug)]
pub enum MatrixError {
    DimensionMismatch,
    NonSquareMatrix,
    SingularMatrix,
    InvalidOperation,
}

impl std::fmt::Display for MatrixError {
    fn fmt(&self, f: &mut std::fmt::Formatter) -> std::fmt::Result {
        match self {
            MatrixError::DimensionMismatch => write!(f, "Matrix dimensions do not match for the requested operation"),
            MatrixError::NonSquareMatrix => write!(f, "Operation requires a square matrix"),
            MatrixError::SingularMatrix => write!(f, "Matrix is singular and cannot be inverted"),
            MatrixError::InvalidOperation => write!(f, "Invalid matrix operation requested"),
        }
    }
}

pub struct MatrixOps;

impl MatrixOps {
    // Process a matrix compute request and return the appropriate response
    pub async fn process_request(
        request: MatrixComputeRequest,
    ) -> MatrixComputeResponse {
        let start_time = Instant::now();
        let task_id = request.task_id.clone();

        // Check if matrix_a exists
        if request.matrix_a.is_none() {
            return MatrixComputeResponse {
                result: None,
                task_id,
                status: "failed".to_string(),
                execution_time_ms: 0,
                error_message: "Missing matrix A".to_string(),
            };
        }

        // Extract matrix_a from the Option
        let matrix_a = request.matrix_a.as_ref().unwrap();

        // Call the appropriate matrix operation based on the request
        let result = match request.operation.as_str() {
            "multiply" => {
                // Check if matrix_b exists for multiplication
                if request.matrix_b.is_none() {
                    Err(MatrixError::DimensionMismatch)
                } else {
                    // Extract matrix_b from the Option
                    let matrix_b = request.matrix_b.as_ref().unwrap();
                    Self::matrix_multiply(matrix_a, matrix_b)
                }
            },
            "transpose" => Self::matrix_transpose(matrix_a),
            "inverse" => Self::matrix_inverse(matrix_a),
            "add" => {
                // Check if matrix_b exists for addition
                if request.matrix_b.is_none() {
                    Err(MatrixError::DimensionMismatch)
                } else {
                    // Extract matrix_b from the Option
                    let matrix_b = request.matrix_b.as_ref().unwrap();
                    Self::matrix_add(matrix_a, matrix_b)
                }
            },
            "subtract" => {
                // Check if matrix_b exists for subtraction
                if request.matrix_b.is_none() {
                    Err(MatrixError::DimensionMismatch)
                } else {
                    // Extract matrix_b from the Option
                    let matrix_b = request.matrix_b.as_ref().unwrap();
                    Self::matrix_subtract(matrix_a, matrix_b)
                }
            },
            _ => Err(MatrixError::InvalidOperation),
        };

        // Create response based on the result
        match result {
            Ok(result_matrix) => {
                let elapsed = start_time.elapsed();
                MatrixComputeResponse {
                    result: Some(result_matrix),
                    task_id,
                    status: "completed".to_string(),
                    execution_time_ms: elapsed.as_millis() as i64,
                    error_message: String::new(),
                }
            }
            Err(err) => {
                MatrixComputeResponse {
                    result: None,
                    task_id,
                    status: "failed".to_string(),
                    execution_time_ms: 0,
                    error_message: err.to_string(),
                }
            }
        }
    }

    // Matrix multiplication: C = A * B
    fn matrix_multiply(a: &Matrix, b: &Matrix) -> Result<Matrix, MatrixError> {
        // Check if dimensions are compatible for multiplication
        if a.cols != b.rows {
            return Err(MatrixError::DimensionMismatch);
        }

        let result_rows = a.rows;
        let result_cols = b.cols;
        let mut result_data = vec![0.0; (result_rows * result_cols) as usize];

        // Perform matrix multiplication
        for i in 0..a.rows as usize {
            for j in 0..b.cols as usize {
                let mut sum = 0.0;
                for k in 0..a.cols as usize {
                    let a_index = i * a.cols as usize + k;
                    let b_index = k * b.cols as usize + j;

                    if a_index < a.data.len() && b_index < b.data.len() {
                        sum += a.data[a_index] * b.data[b_index];
                    }
                }
                result_data[i * result_cols as usize + j] = sum;
            }
        }

        Ok(Matrix {
            rows: result_rows,
            cols: result_cols,
            data: result_data,
        })
    }

    // Matrix transpose: B = A^T
    fn matrix_transpose(a: &Matrix) -> Result<Matrix, MatrixError> {
        let result_rows = a.cols;
        let result_cols = a.rows;
        let mut result_data = vec![0.0; (result_rows * result_cols) as usize];

        // Perform matrix transposition
        for i in 0..a.rows as usize {
            for j in 0..a.cols as usize {
                // Fix: Using result_cols instead of result_rows for indexing
                result_data[j * result_cols as usize + i] = a.data[i * a.cols as usize + j];
            }
        }

        Ok(Matrix {
            rows: result_rows,
            cols: result_cols,
            data: result_data,
        })
    }

    // Matrix inverse: B = A^(-1)
    fn matrix_inverse(a: &Matrix) -> Result<Matrix, MatrixError> {
        // Check if the matrix is square
        if a.rows != a.cols {
            return Err(MatrixError::NonSquareMatrix);
        }

        let n = a.rows as usize;

        // For 2x2 matrices, we can use the analytical formula
        if n == 2 {
            let a11 = a.data[0];
            let a12 = a.data[1];
            let a21 = a.data[2];
            let a22 = a.data[3];

            let det = a11 * a22 - a12 * a21;

            if det.abs() < 1e-10 {
                return Err(MatrixError::SingularMatrix);
            }

            let inv_det = 1.0 / det;

            let result_data = vec![
                a22 * inv_det, -a12 * inv_det,
                -a21 * inv_det, a11 * inv_det
            ];

            return Ok(Matrix {
                rows: 2,
                cols: 2,
                data: result_data,
            });
        }

        // For larger matrices, we would implement a more complex algorithm like
        // Gaussian elimination or LU decomposition. For this MVP, we'll support only 2x2.
        Err(MatrixError::InvalidOperation)
    }

    // Matrix addition: C = A + B
    fn matrix_add(a: &Matrix, b: &Matrix) -> Result<Matrix, MatrixError> {
        // Check if dimensions match
        if a.rows != b.rows || a.cols != b.cols {
            return Err(MatrixError::DimensionMismatch);
        }

        // Perform element-wise addition
        let result_data: Vec<f32> = a.data
            .iter()
            .zip(b.data.iter())
            .map(|(a_val, b_val)| a_val + b_val)
            .collect();

        Ok(Matrix {
            rows: a.rows,
            cols: a.cols,
            data: result_data,
        })
    }

    // Matrix subtraction: C = A - B
    fn matrix_subtract(a: &Matrix, b: &Matrix) -> Result<Matrix, MatrixError> {
        // Check if dimensions match
        if a.rows != b.rows || a.cols != b.cols {
            return Err(MatrixError::DimensionMismatch);
        }

        // Perform element-wise subtraction
        let result_data: Vec<f32> = a.data
            .iter()
            .zip(b.data.iter())
            .map(|(a_val, b_val)| a_val - b_val)
            .collect();

        Ok(Matrix {
            rows: a.rows,
            cols: a.cols,
            data: result_data,
        })
    }
}
