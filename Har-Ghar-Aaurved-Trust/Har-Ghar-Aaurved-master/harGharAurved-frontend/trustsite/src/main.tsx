import { ChakraProvider, extendTheme } from "@chakra-ui/react";
//import { ThemeProvider, createTheme } from "@mui/material/styles";
import React from "react";
import ReactDOM from "react-dom/client";
import App from "./App";
import { BrowserRouter } from "react-router-dom";

// Define your custom Chakra UI theme
const chakraTheme = extendTheme({
  colors: {
    brand: {
      50: "#f0e4ff",
      100: "#cbb2ff",
      200: "#a480ff",
      300: "#7a4dff",
      400: "#641bfe",
      500: "#5a01e5",
      600: "#5200b3",
      700: "#430081",
      800: "#2d004f",
      900: "#14001f",
    },
  },
  fonts: {
    heading: `'Inter', sans-serif`,
    body: `'Inter', sans-serif`,
  },
});

// Define your custom MUI theme
// const muiTheme = createTheme({
  // Define your MUI theme options here
// });

// Render the root component of your application
//<ThemeProvider theme={muiTheme}></ThemeProvider>
ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <ChakraProvider theme={chakraTheme}>
        <BrowserRouter>
          <App />
        </BrowserRouter>
    </ChakraProvider>
  </React.StrictMode>
);
