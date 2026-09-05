// Main Express server entry point
const express = require("express");
const cors = require("cors");
const foodRouter = require("./food.router");

const app = express();

app.use(cors());

// Mount mock image API routes
app.use("/api/food", foodRouter);

const PORT = 9002;
app.listen(PORT, () => {
  console.log(`Mock server running on http://localhost:${PORT}`);
});
