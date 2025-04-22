use std::thread;
use std::time::Duration;
use std::time::Instant;
use tonic::Request;

// Use the shared library
use ml::{ml_client::MlClient, ComputeRequest, Matrix, MatrixComputeRequest, StatusRequest};
use ml_gpu_service_lib::ml;

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    println!("Connecting to ML Server...");

    let mut client = MlClient::connect("http://localhost:50051").await?;
    println!("Connection established");

    // Test the status endpoint
    test_status(&mut client).await?;

    // Test the compute endpoint
    test_compute(&mut client, "multiply").await?;

    // Wait a bit
    thread::sleep(Duration::from_millis(500));

    // Test another computation type
    test_compute(&mut client, "square").await?;

    Ok(())
}

async fn test_status(
    client: &mut MlClient<tonic::transport::Channel>,
) -> Result<(), Box<dyn std::error::Error>> {
    println!("\n=== Testing Status Endpoint ===");
    let request = Request::new(StatusRequest {
        client_id: "rust-test-client".to_string(),
    });

    match client.status(request).await {
        Ok(response) => {
            let status = response.into_inner();
            println!("Status Response:");
            println!("Ready: {}", status.ready);
            println!("GPU Info: {}", status.gpu_info);
            println!("Current Load: {}", status.current_load);
            if !status.error.is_empty() {
                println!("Error: {}", status.error);
            }
        }
        Err(e) => println!("Error getting status: {}", e),
    }

    Ok(())
}

async fn test_compute(
    client: &mut MlClient<tonic::transport::Channel>,
    task_type: &str,
) -> Result<(), Box<dyn std::error::Error>> {
    println!("\n=== Testing Compute Endpoint with {} ===", task_type);

    // Create sample data
    let data = vec![1.0, 2.0, 3.0, 4.0, 5.0];
    println!("Input data: {:?}", data);

    let request = Request::new(ComputeRequest {
        data: data.clone(),
        task_id: format!("test-{}-{}", task_type, chrono::Utc::now().timestamp()),
        task_type: task_type.to_string(),
    });

    let start = Instant::now();
    match client.compute(request).await {
        Ok(response) => {
            let compute_response = response.into_inner();
            let elapsed = start.elapsed();

            println!("Compute Response:");
            println!("Task ID: {}", compute_response.task_id);
            println!("Status: {}", compute_response.status);
            println!(
                "Execution Time (ms): {}",
                compute_response.execution_time_ms
            );
            println!("Result: {:?}", compute_response.result);
            println!("Client measured time: {:?}", elapsed);

            // Verify the result
            if task_type == "multiply" {
                let expected: Vec<f32> = data.iter().map(|x| x * 2.0).collect();
                println!("Expected result: {:?}", expected);
                assert_eq!(
                    compute_response.result, expected,
                    "Result doesn't match expected output"
                );
            } else if task_type == "square" {
                let expected: Vec<f32> = data.iter().map(|x| x * x).collect();
                println!("Expected result: {:?}", expected);
                assert_eq!(
                    compute_response.result, expected,
                    "Result doesn't match expected output"
                );
            }

            println!("Verification: Success!");
        }
        Err(e) => println!("Error in computation: {}", e),
    }

    Ok(())
}
