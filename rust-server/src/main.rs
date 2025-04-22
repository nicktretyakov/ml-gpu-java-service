use std::sync::Arc;
use std::net::SocketAddr;
use tonic::{transport::Server, Request, Response, Status};

// Use the shared library
use ml_gpu_service_lib::ml;
use ml_gpu_service_lib::gpu_compute::GpuCompute;
use ml_gpu_service_lib::matrix_ops::MatrixOps;

use ml::{ml_server::{Ml, MlServer}, ComputeRequest, ComputeResponse, StatusRequest, StatusResponse, 
         MatrixComputeRequest, MatrixComputeResponse};
use tokio::sync::Mutex;

// Define the ML service
#[derive(Debug)]
pub struct MLService {
    gpu_compute: Arc<Mutex<GpuCompute>>,
}

impl Default for MLService {
    fn default() -> Self {
        Self {
            gpu_compute: Arc::new(Mutex::new(GpuCompute::default())),
        }
    }
}

#[tonic::async_trait]
impl Ml for MLService {
    async fn compute(
        &self,
        request: Request<ComputeRequest>,
    ) -> Result<Response<ComputeResponse>, Status> {
        println!("Received compute request");
        
        // Extract the data from the request
        let compute_request = request.into_inner();
        let data = compute_request.data;
        let task_id = compute_request.task_id.clone();
        let task_type = compute_request.task_type;
        
        println!("Task ID: {}, Type: {}, Data size: {}", task_id, task_type, data.len());
        
        // Perform the GPU computation
        let result = self.run_gpu_task(data, &task_type).await;
        
        // Create and return the response
        let response = ComputeResponse {
            result,
            task_id: task_id.clone(),
            status: "completed".to_string(),
            execution_time_ms: 100, // Simulated execution time
        };
        
        println!("Computation completed for task: {}", task_id);
        Ok(Response::new(response))
    }
    
    async fn matrix_compute(
        &self,
        request: Request<MatrixComputeRequest>,
    ) -> Result<Response<MatrixComputeResponse>, Status> {
        println!("Received matrix compute request");
        
        // Extract the matrix compute request
        let matrix_request = request.into_inner();
        let task_id = matrix_request.task_id.clone();
        let operation = matrix_request.operation.clone();
        
        // Log basic information about the matrices
        if let Some(ref matrix_a) = matrix_request.matrix_a {
            println!("Matrix A: {}x{}, Data size: {}", matrix_a.rows, matrix_a.cols, matrix_a.data.len());
        }
        
        if let Some(ref matrix_b) = matrix_request.matrix_b {
            println!("Matrix B: {}x{}, Data size: {}", matrix_b.rows, matrix_b.cols, matrix_b.data.len());
        }
        
        println!("Task ID: {}, Operation: {}", task_id, operation);
        
        // Process the matrix operation request
        let response = MatrixOps::process_request(matrix_request).await;
        
        if response.status == "completed" {
            println!("Matrix computation completed successfully for task: {}", task_id);
        } else {
            println!("Matrix computation failed for task: {}: {}", task_id, response.error_message);
        }
        
        Ok(Response::new(response))
    }
    
    async fn status(
        &self,
        request: Request<StatusRequest>,
    ) -> Result<Response<StatusResponse>, Status> {
        let client_id = request.into_inner().client_id;
        println!("Received status request from client: {}", client_id);
        
        // Create a status response
        let response = StatusResponse {
            ready: true,
            gpu_info: "Simulated GPU Device".to_string(),
            current_load: 0.1,
            error: "".to_string(),
        };
        
        Ok(Response::new(response))
    }
}

impl MLService {
    async fn run_gpu_task(&self, data: Vec<f32>, task_type: &str) -> Vec<f32> {
        println!("Running GPU task for type: {}", task_type);
        
        // Get lock on GPU compute resources
        let gpu_compute = self.gpu_compute.lock().await;
        
        // Perform the computation
        let result = gpu_compute.compute(&data, task_type).await;
        
        println!("GPU computation completed with {} results", result.len());
        result
    }

    // Initialize GPU resources
    async fn init_gpu(&self) -> Result<(), String> {
        let mut gpu_compute = self.gpu_compute.lock().await;
        gpu_compute.init_gpu().await
    }
}

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    // Set up logging
    env_logger::init();
    println!("Starting ML GPU Server...");
    
    // Create and initialize the ML service
    let ml_service = MLService::default();
    
    // Initialize GPU resources
    if let Err(e) = ml_service.init_gpu().await {
        println!("Failed to initialize GPU resources: {}", e);
        return Err(e.into());
    }
    
    // Set up the server address
    let addr: SocketAddr = "0.0.0.0:50051".parse().expect("Invalid address");
    println!("ML GPU Server running on {}", addr);

    // Start the gRPC server
    Server::builder()
        .add_service(MlServer::new(ml_service))
        .serve(addr)
        .await?;

    Ok(())
}
