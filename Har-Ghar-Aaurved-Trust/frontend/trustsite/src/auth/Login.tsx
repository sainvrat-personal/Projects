import {
  Avatar,
  Box,
  Button,
  Container,
  FormControl,
  FormLabel,
  Input,
  Text,
  FormErrorMessage,
  Grid
} from "@chakra-ui/react";
import { useState } from "react";
import { Link } from "react-router-dom";

const Login = () => {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");

  const handleLogin = () => {
    // Mock validation - check if email and password are provided
    if (!email || !password) {
      setError("Please provide both email and password.");
      return;
    }

    // Implement login logic here
    console.log("Logging in with:", email, password);
  };
//bgImage="url('../../public/logo400.png')"
  return (
    <Box
      bgColor="green.50"
      bgSize="cover"
      bgPosition="center"
      minH="100vh"
      display="flex"
      justifyContent="center; space-between"
      alignItems="center"
    >
      <Container maxW="xs" p={8} borderRadius="md" backgroundColor="green.500" textColor="green.50">
        <Box textAlign="center">
          <Avatar size="lg" bg="green.700">
          </Avatar>
          <Box mt={4}>
            <FormControl isInvalid={!!error}>
              <FormLabel textColor="green.50">Email Address</FormLabel>
              <Input
                id = "email-input-field"
                type="email"
                value={email}
                placeholder="Email Address"
                onChange={(e) => setEmail(e.target.value)}
                className="form-control"
              />
              <FormLabel textColor="green.50" mt={2}>Password</FormLabel>
              <Input
                id = "password-input-field"
                type="password"
                placeholder="Password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="form-control"
              />
              <FormErrorMessage>{error}</FormErrorMessage>
            </FormControl>
            <Button
              colorScheme="primary"
              textColor="green.50"
              bg="green.400"
              size="md"
              mt={4}
              onClick={handleLogin}
              width="100%"
              className="btn btn-primary"
            >
              Login
            </Button>
            <Box mt={2} display="flex" justifyContent="space-between">
            <Text mt={2} fontSize="sm" textAlign="left" textColor="green.50">
              <Link to="/signup" className="text-primary">
                Signup
              </Link>
            </Text>
            <Text mt={2} fontSize="sm" textAlign="right" textColor="green.50">
              <Link to="/resetPassword" className="text-primary">
                forgot password?
              </Link>
            </Text>
            </Box>
            <Grid mt={2} templateColumns="1fr">
              <Text fontSize="sm" textColor="green.50">
                <Link to="/#">Home</Link>
              </Text>
            </Grid>
          </Box>
        </Box>
      </Container>
    </Box>
  );
};

export default Login;