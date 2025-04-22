// WebSocket connection
let socket;
let resultChart;
let currentTaskId = null;
let taskStatus = {};

let chartData = {
  labels: [],
  datasets: [
    {
      label: "Input Data",
      data: [],
      backgroundColor: "rgba(54, 162, 235, 0.2)",
      borderColor: "rgba(54, 162, 235, 1)",
      borderWidth: 1,
    },
    {
      label: "Result Data",
      data: [],
      backgroundColor: "rgba(255, 99, 132, 0.2)",
      borderColor: "rgba(255, 99, 132, 1)",
      borderWidth: 1,
    },
  ],
};

// Connect to WebSocket
function connectWebSocket() {
  const protocol = window.location.protocol === "https:" ? "wss:" : "ws:";
  const wsUrl = `${protocol}//${window.location.host}/ws`;

  console.log(`Connecting to WebSocket at ${wsUrl}`);
  socket = new WebSocket(wsUrl);

  socket.onopen = function () {
    console.log("WebSocket connection established");
    document.getElementById("status-indicator").className = "connected";
    document.getElementById("connection-status").textContent = "Connected";
    logMessage("Connected to ML Hybrid System");
  };

  socket.onmessage = function (event) {
    console.log("Message received:", event.data);

    // Try to parse the message as JSON
    try {
      const data = JSON.parse(event.data);

      if (data.taskId) {
        // Store task status
        taskStatus[data.taskId] = data.status;

        // Format message based on status
        let message = `Task ${data.taskId}: ${data.status}`;

        if (data.status === "started") {
          logMessage(`${message} - Processing ${data.data.length} data points`);
          if (data.taskId === currentTaskId) {
            updateInputData(data.data);
          }
        } else if (data.status === "completed") {
          logMessage(`${message} - Generated ${data.data.length} results`);
          if (data.taskId === currentTaskId) {
            updateResultData(data.data);
          }
        } else if (data.status === "error") {
          logMessage(`${message} - Error: ${data.data.error}`);
        }
      } else {
        // Regular message
        logMessage(data.toString());
      }
    } catch (e) {
      // Not JSON, just a regular message
      logMessage(event.data);
    }
  };

  socket.onclose = function () {
    console.log("WebSocket connection closed");
    document.getElementById("status-indicator").className = "disconnected";
    document.getElementById("connection-status").textContent = "Disconnected";
    logMessage("Disconnected from server");

    // Try to reconnect after a delay
    setTimeout(connectWebSocket, 5000);
  };

  socket.onerror = function (error) {
    console.error("WebSocket error:", error);
    logMessage("WebSocket Error: " + (error.message || "Unknown error"));
  };
}

// Log a message to the UI
function logMessage(message) {
  const log = document.getElementById("log");
  const messageElement = document.createElement("div");
  messageElement.className = "log-message";

  const timestamp = new Date().toLocaleTimeString();
  messageElement.textContent = `[${timestamp}] ${message}`;

  log.appendChild(messageElement);
  log.scrollTop = log.scrollHeight;
}

// Submit the computation request
async function submitComputation(event) {
  event.preventDefault();

  const dataInput = document.getElementById("data-input").value;
  const taskType = document.getElementById("task-type").value;

  // Parse the input data
  try {
    const data = dataInput
      .split(",")
      .map((num) => parseFloat(num.trim()))
      .filter((num) => !isNaN(num));

    if (data.length === 0) {
      logMessage("Error: Please provide valid numeric data");
      return;
    }

    // Show loading state
    const submitButton = event.target.querySelector('button[type="submit"]');
    const originalText = submitButton.textContent;
    submitButton.textContent = "Processing...";
    submitButton.disabled = true;

    try {
      // Send the request to the server
      const response = await fetch("/api/ml/compute", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          data: data,
          taskType: taskType,
        }),
      });

      if (response.ok) {
        const result = await response.json();
        currentTaskId = result.taskId;

        logMessage(`Computation request sent. Task ID: ${result.taskId}`);

        // If we received results directly (not waiting for WebSocket)
        if (
          result.result &&
          result.result.length > 0 &&
          result.status === "completed"
        ) {
          updateInputData(data);
          updateResultData(result.result);
        }
      } else {
        const error = await response.text();
        logMessage(`Error: ${error}`);
      }
    } finally {
      // Restore button state
      submitButton.textContent = originalText;
      submitButton.disabled = false;
    }
  } catch (error) {
    logMessage(`Error: ${error.message}`);
  }
}

// Initialize the chart
function initChart() {
  const ctx = document.getElementById("result-chart").getContext("2d");
  resultChart = new Chart(ctx, {
    type: "bar",
    data: chartData,
    options: {
      responsive: true,
      scales: {
        y: {
          beginAtZero: true,
        },
      },
      plugins: {
        legend: {
          display: true,
          position: "top",
        },
        tooltip: {
          callbacks: {
            label: function (context) {
              return `${context.dataset.label}: ${context.parsed.y.toFixed(2)}`;
            },
          },
        },
      },
    },
  });
}

// Update the input data in the chart
function updateInputData(data) {
  // Create labels if needed
  if (chartData.labels.length !== data.length) {
    chartData.labels = data.map((_, i) => `Item ${i + 1}`);
  }

  // Update the input dataset
  chartData.datasets[0].data = data;

  // Clear the result dataset if not already populated
  if (
    !chartData.datasets[1].data.length ||
    chartData.datasets[1].data.some((d) => d === null)
  ) {
    chartData.datasets[1].data = Array(data.length).fill(null);
  }

  // Update the chart
  resultChart.update();
}

// Update the result data in the chart
function updateResultData(data) {
  // Update the result dataset
  chartData.datasets[1].data = data;

  // Update the chart
  resultChart.update();
}

// Update both datasets in the chart
function updateChart(data) {
  if (Array.isArray(data)) {
    updateResultData(data);
  }
}

// Clear the log
function clearLog() {
  document.getElementById("log").innerHTML = "";
  logMessage("Log cleared");
}

// Check system status
async function checkSystemStatus() {
  try {
    const response = await fetch("/api/ml/status");
    if (response.ok) {
      const text = await response.text();
      logMessage(`System status: ${text}`);
      return true;
    } else {
      logMessage("System status check failed");
      return false;
    }
  } catch (error) {
    logMessage(`Error checking system status: ${error.message}`);
    return false;
  }
}

// Get system info
async function getSystemInfo() {
  try {
    const response = await fetch("/api/ml/info");
    if (response.ok) {
      const info = await response.json();
      logMessage(`System: ${info.system} v${info.version}`);
      logMessage(`Description: ${info.description}`);
      return info;
    }
  } catch (error) {
    console.error("Error fetching system info:", error);
  }
  return null;
}

// Initialize when the document is loaded
document.addEventListener("DOMContentLoaded", function () {
  // Initialize the chart
  initChart();

  // Connect to WebSocket
  connectWebSocket();

  // Set up event listeners
  document
    .getElementById("compute-form")
    .addEventListener("submit", submitComputation);
  document.getElementById("clear-log").addEventListener("click", clearLog);

  // Populate the data input with some example data
  document.getElementById("data-input").value =
    "1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0";

  // Check system status
  setTimeout(async () => {
    await checkSystemStatus();
    await getSystemInfo();
  }, 1000);
});
