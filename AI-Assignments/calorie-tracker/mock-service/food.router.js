// Mock API route for image analysis

const express = require('express');
const multer = require('multer');

// Use memory storage so images are not saved to disk (image is discarded after request)
const upload = multer({ storage: multer.memoryStorage() });

const router = express.Router();

// In-memory list of sample food items with estimated calories
const sampleFoods = [
  { description: "Pizza", estimatedCalories: 200 },
  { description: "Burger", estimatedCalories: 350 },
  { description: "Pasta", estimatedCalories: 250 },
  { description: "Salad", estimatedCalories: 80 },
  { description: "Sushi", estimatedCalories: 150 },
  { description: "Ice Cream", estimatedCalories: 180 },
  { description: "Fried Chicken", estimatedCalories: 320 },
  { description: "Sandwich", estimatedCalories: 220 },
  { description: "Steak", estimatedCalories: 400 },
  { description: "Fries", estimatedCalories: 300 },
  { description: "Tacos", estimatedCalories: 210 },
  { description: "Doughnut", estimatedCalories: 260 },
  { description: "Soup", estimatedCalories: 90 },
  { description: "Rice Bowl", estimatedCalories: 230 },
  { description: "Noodles", estimatedCalories: 270 },
  { description: "Pancakes", estimatedCalories: 190 },
  { description: "Omelette", estimatedCalories: 160 },
  { description: "Apple Pie", estimatedCalories: 240 },
  { description: "Hot Dog", estimatedCalories: 280 },
  { description: "Falafel", estimatedCalories: 170 }
];

// This endpoint pretends to accept an image but does nothing with it
router.post('/analyze', upload.single('image'), (req, res) => {
  // Throw error if image is not uploaded
  if (!req.file) {
    return res.status(400).json({ error: "No image uploaded" });
  }

  // Randomly throw error that food item was not recognized (20% chance)
  if (Math.random() < 0.2) {
    return res.status(422).json({ error: "Food item could not be recognized" });
  }

  // Randomly select a food item from the list
  const randomFood = sampleFoods[Math.floor(Math.random() * sampleFoods.length)];
  res.json(randomFood);
});

module.exports = router;