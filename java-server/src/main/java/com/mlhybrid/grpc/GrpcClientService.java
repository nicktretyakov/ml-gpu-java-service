package com.mlhybrid.grpc;

import com.mlhybrid.grpc.ComputeRequest;
import com.mlhybrid.grpc.ComputeResponse;
import com.mlhybrid.grpc.MLGrpc;
import com.mlhybrid.grpc.MatrixComputeRequest;
import com.mlhybrid.grpc.MatrixComputeResponse;
import com.mlhybrid.model.Matrix;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GrpcClientService {

    private static final Logger logger = LoggerFactory.getLogger(
        GrpcClientService.class
    );

    private ManagedChannel channel;
    private MLGrpc.MLBlockingStub blockingStub;

    @Value("${grpc.server.host:localhost}")
    private String grpcServerHost;

    @Value("${grpc.server.port:50051}")
    private int grpcServerPort;

    @PostConstruct
    public void init() {
        logger.info(
            "Initializing gRPC client to connect to {}:{}",
            grpcServerHost,
            grpcServerPort
        );
        channel = ManagedChannelBuilder.forAddress(
            grpcServerHost,
            grpcServerPort
        )
            .usePlaintext()
            .build();
        blockingStub = MLGrpc.newBlockingStub(channel);
        logger.info("gRPC client initialized successfully");
    }

    @PreDestroy
    public void shutdown() throws InterruptedException {
        if (channel != null) {
            logger.info("Shutting down gRPC channel");
            channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    public com.mlhybrid.model.ComputeResponse compute(
        List<Float> data,
        String taskId,
        String taskType
    ) {
        // Build the gRPC request
        ComputeRequest request = ComputeRequest.newBuilder()
            .addAllData(data)
            .setTaskId(taskId)
            .setTaskType(taskType)
            .build();

        // Call the Rust service
        logger.info(
            "Sending compute request to Rust server for task: {}",
            taskId
        );
        try {
            ComputeResponse response = blockingStub.compute(request);
            logger.info(
                "Received response from Rust server for task: {}",
                taskId
            );

            // Convert the gRPC response to our model
            return new com.mlhybrid.model.ComputeResponse(
                response.getResultList(),
                response.getTaskId(),
                response.getStatus()
            );
        } catch (Exception e) {
            logger.error(
                "Error calling Rust gRPC service: {}",
                e.getMessage(),
                e
            );
            throw new RuntimeException(
                "Error processing computation request",
                e
            );
        }
    }

    public com.mlhybrid.model.MatrixComputeResponse matrixCompute(
        com.mlhybrid.model.MatrixComputeRequest request
    ) {
        // Build the gRPC request
        com.mlhybrid.grpc.Matrix matrixA = null;
        if (request.getMatrixA() != null) {
            matrixA = request.getMatrixA().toGrpcMatrix();
        }

        com.mlhybrid.grpc.Matrix matrixB = null;
        if (request.getMatrixB() != null) {
            matrixB = request.getMatrixB().toGrpcMatrix();
        }

        MatrixComputeRequest grpcRequest = MatrixComputeRequest.newBuilder()
            .setTaskId(request.getTaskId())
            .setOperation(request.getOperation())
            .setMatrixA(matrixA)
            .setMatrixB(matrixB)
            .build();

        // Call the Rust service
        logger.info(
            "Sending matrix compute request to Rust server for task: {}, operation: {}",
            request.getTaskId(),
            request.getOperation()
        );
        try {
            MatrixComputeResponse grpcResponse = blockingStub.matrixCompute(
                grpcRequest
            );
            logger.info(
                "Received matrix response from Rust server for task: {}",
                request.getTaskId()
            );

            // Convert the gRPC response to our model
            com.mlhybrid.model.Matrix resultMatrix = null;
            if (grpcResponse.hasResult()) {
                resultMatrix = Matrix.fromGrpcMatrix(grpcResponse.getResult());
            }

            return new com.mlhybrid.model.MatrixComputeResponse(
                resultMatrix,
                grpcResponse.getTaskId(),
                grpcResponse.getStatus(),
                grpcResponse.getErrorMessage(),
                grpcResponse.getExecutionTimeMs()
            );
        } catch (Exception e) {
            logger.error(
                "Error calling Rust gRPC service for matrix operation: {}",
                e.getMessage(),
                e
            );
            throw new RuntimeException(
                "Error processing matrix computation request",
                e
            );
        }
    }
}
