Java matrix operation models and added gRPC integration
Implemented matrix computation functionality in Rust with GPU acceleration
Enhanced WebSocket system for real-time updates
Added web UI for matrix operations

/java-server  mvn spring-boot:run &

/rust-server  cargo run --bin ml-matrix-client

/rust-server  cargo run --bin ml-client

/rust-server  cargo run --bin ml-server

# ML GPU Java Service

A hybrid, cross-language ML inference platform showcasing GPU-accelerated computation in Rust with a Java gRPC/WebSocket frontend and web UI.

---

## Table of Contents
1. [Overview](#overview)
2. [Components](#components)
3. [Architecture](#architecture)
4. [Prerequisites](#prerequisites)
5. [Building the Project](#building-the-project)
6. [Configuration](#configuration)
7. [Running the Services](#running-the-services)
8. [gRPC API & Testing](#grpc-api--testing)
9. [Directory Structure](#directory-structure)
10. [Contributing](#contributing)
11. [License](#license)

---

## Overview

`ml-gpu-java-service` demonstrates a full-stack ML hybrid architecture:

- **Rust ML Server** (`rust-server`): Implements a gRPC service performing compute and matrix operations with GPU acceleration (via `wgpu`).
- **Java Spring Boot Server** (`java-server`): Exposes a web dashboard, REST/WebSocket endpoints, and proxies requests to the Rust service over gRPC.
- **Java Example App** (`java-app`): A simple Spring Boot application illustrating client-side usage of the hybrid ML system.
- **gRPC Definitions** (`proto/ml.proto`): Defines `Compute`, `MatrixCompute`, and `Status` RPCs.
- **Rust Clients** (`ml-client`, `ml-matrix-client`): Command-line tools for invoking the Rust service directly.
- **Web UI** (`java-server/resources/static`): HTML/JS dashboard for submitting ML tasks and visualizing results in real-time.


## Components

| Directory      | Description                                                                   |
|----------------|-------------------------------------------------------------------------------|
| `proto/`       | Protobuf definitions for the gRPC service.                                     |
| `rust-server/` | Rust-based ML server with GPU acceleration and gRPC implementation.           |
| `java-server/` | Java Spring Boot server offering a web dashboard, WebSocket, and gRPC proxy. |
| `java-app/`    | Sample Java application demonstrating standalone usage of the hybrid system.   |
| `grpcurl/`     | Placeholder for `grpcurl` testing scripts.                                     |


## Architecture

```text
    +-------------+        gRPC        +---------------+      WebSocket     +---------+
    | Web Browser | <----------------- | Java Server   | -----------------> | Browser |
    |  (HTML/JS)  |                     | (Spring Boot) |                     | Dashboard|
    +-------------+                     +---------------+                     +---------+
        |                                                              ^
        | HTTP Submit / WebSocket updates                              |
        v                                                              |
 +------------+     gRPC      +-------------+      GPU Compute        +---------+
 | Java App   | <----------> | Rust Server |<---------------------> | GPU     |
 | (Optional) |              | (wgpu,tonic) |                        | Device  |
 +------------+              +-------------+                        +---------+

```

1. **Client** submits a computation request via REST or WebSocket to the Java server.
2. **Java Server** forwards the request over gRPC to the Rust server.
3. **Rust Server** executes compute/matrix operations on the GPU and returns results.
4. **Java Server** pushes real-time updates back to the web UI via WebSocket.


## Prerequisites

- **Rust** (>= 1.60) toolchain
- **Java 11** and **Maven** (>= 3.6)
- **protoc** (Protocol Buffers compiler)
- **GPU drivers** supporting Vulkan/Metal/DX12 (for `wgpu`)


## Building the Project

### 1. Generate gRPC Code from Protobuf

- **Rust**: Uses `build.rs` and `tonic-build`; runs automatically during `cargo build`.
- **Java**: Protobuf classes generated via `protobuf-maven-plugin` in the `java-server` build.


### 2. Build Rust Components

```bash
cd rust-server
cargo build --release
```

This produces:
- `target/release/ml-server` (the gRPC GPU server)
- `target/release/ml-client` (CLI for `Compute`)
- `target/release/ml-matrix-client` (CLI for `MatrixCompute`)


### 3. Build Java Services

```bash
# Java Server
cd java-server
tmvn clean package  # or mvn spring-boot:run for live dev

# Java App
cd ../java-app
mvn clean package  # or mvn spring-boot:run
```


## Configuration

| Service      | Property                 | Default       | Description                                     |
|--------------|--------------------------|---------------|-------------------------------------------------|
| `java-server`| `server.port`            | `8000`        | HTTP port for web UI and WebSocket.
|              | `grpc.server.host`       | `localhost`   | Host for gRPC calls to Rust server.
|              | `grpc.server.port`       | `50051`       | gRPC port on Rust server.
|              | (in `application.properties`)                       |

_No additional config required for `rust-server` (listens on `50051` by default)._  


## Running the Services

1. **Start Rust GPU Server**

   ```bash
   cd rust-server
   ./target/release/ml-server
   ```

2. **Start Java Server**

   ```bash
   cd java-server
   mvn spring-boot:run
   ```

3. **(Optional) Start Java Example App**

   ```bash
   cd java-app
   mvn spring-boot:run
   ```

4. **Open the Web UI**

   Navigate to `http://localhost:8000/` in your browser.


## gRPC API & Testing

The `ml.proto` defines the `ML` service:
```proto
service ML {
  rpc Compute(ComputeRequest) returns (ComputeResponse);
  rpc MatrixCompute(MatrixComputeRequest) returns (MatrixComputeResponse);
  rpc Status(StatusRequest) returns (StatusResponse);
}
```

### Example using `grpcurl`
```bash
# Check server status
grpcurl -plaintext localhost:50051 ml.ML/Status

# Perform vector compute
grpcurl -plaintext -d '{"data": [1,2,3], "taskType": "square", "taskId": "t1"}' localhost:50051 ml.ML/Compute

# Perform matrix multiplication
grpcurl -plaintext -d '{"matrixA": {"rows":2,"cols":2,"data":[1,2,3,4]},"matrixB": {"rows":2,"cols":2,"data":[5,6,7,8]},"operation":"multiply","taskId":"m1"}' localhost:50051 ml.ML/MatrixCompute
```


## Directory Structure

```
ml-gpu-java-service/
├── proto/                 # gRPC definitions (ml.proto)
├── rust-server/           # Rust GPU-accelerated gRPC server
│   ├── src/
│   ├── build.rs
│   └── Cargo.toml
├── java-server/           # Java Spring Boot web/dashboard server
│   ├── src/main/java/com/mlhybrid/
│   ├── resources/
│   │   ├── application.properties
│   │   └── static/        # HTML/CSS/JS dashboard
│   └── pom.xml
├── java-app/              # Example Java client application
│   └── pom.xml
├── grpcurl/               # Scripts or examples for grpcurl
├── .gitignore
└── README.md              # This documentation
```


## Contributing

Contributions are welcome! Please open issues or pull requests and adhere to Java 11 / Rust 2021 coding conventions. Ensure gRPC contract compatibility when modifying `ml.proto`.


## License

This project is licensed under the MIT License. See [LICENSE](LICENSE) for details.

