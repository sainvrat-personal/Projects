import {
  Avatar,
  Box,
  Button,
  Container,
  Grid,
  Input,
  Heading,
  Text,
  FormControl,
  FormLabel,
  FormErrorMessage
} from "@chakra-ui/react";
import { useState } from "react";
import { Link } from "react-router-dom";

const Signup = () => {
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");

  const handleRegister = async () => {
    if (!email || !password) {
      setError("Please provide both email and password.");
      return;
    }
    // Implement your registration logic here
    console.log("Registering with:", name, email, password);
  };
//    bgImage="url('../../public/logo400.png')"
  return (
    <Box
    bgColor="green.50"
    bgSize="cover"
    bgPosition="center"
    minH="100vh"
    display="flex"
    justifyContent="center"
    alignItems="center"
  >
    <Container maxW="xs" p={8} borderRadius="md" backgroundColor="green.500" textColor="green.50">
      <Box mt={20} textAlign="center">
        <Avatar bg="green.700" size="md">
        </Avatar>
        <Heading as="h2" size="xl" mt={4} textColor="green.50">
          Signup
        </Heading>
        <Box mt={4}>
          <FormControl isInvalid={!!error}>
          <FormLabel textColor="green.50" mt={2}>Name</FormLabel>
            <Input
              id = "name-input-field"
              type="text"
              placeholder="Name"
              size="md"
              value={name}
              onChange={(e) => setName(e.target.value)}
              mt={2} // Adjust margin top for spacing
            />
            <FormLabel textColor="green.50" mt={2}>Email Address</FormLabel>
            <Input
              id = "email-input-field"
              type="email"
              placeholder="Email Address"
              size="md"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              mt={2} // Adjust margin top for spacing
            />
            <FormLabel textColor="green.50" mt={2} >Password</FormLabel>
            <Input
              id = "password-input-field"
              type="password"
              placeholder="Password"
              size="md"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              mt={2} // Adjust margin top for spacing
            />
            <FormErrorMessage>{error}</FormErrorMessage>
          </FormControl>
          <Button
            colorScheme="green.400"
            textColor="green.50"
            bg="green.400"
            size="md"
            mt={3}
            onClick={handleRegister}
            width="100%"
          >
            Signup
          </Button>
          <Grid mt={2} templateColumns="1fr">
            <Text fontSize="sm" textColor="green.50">
              <Link to="/login">Already have an account? Login</Link>
            </Text>
          </Grid>
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

export default Signup;