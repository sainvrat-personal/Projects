import {
    Container,
    Box,
    Avatar,
    Heading,
    Input,
    Button,
    Grid,
    Text,
    FormControl,
    FormLabel,
    FormErrorMessage
  } from "@chakra-ui/react";
  import { useState } from "react";
  import { Link } from "react-router-dom";
  
  const PaymentAcceptance = () => {
    const [email, setEmail] = useState("");
    const [paymentStatus, setPaymentStatus] = useState("");
    const [error, setError] = useState("");
  
  
    const updatePaymentStatus = () => {
      if (!email || !paymentStatus) {
        setError("Please provide both email and paymentStatus.");
        return;
      }
      console.log("Payment Accepted for user with email : ", email, paymentStatus);
    };
  //      bgImage="url('../../public/logo400.png')"
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
        <Box mt={20} textAlign="center">
          <Avatar bg="green.700" size="md">
          </Avatar>
          <Heading as="h2" size="xl" mt={4} textColor="green.50">
            Accept Payment
          </Heading>
          <Box mt={4}>
          <FormControl isInvalid={!!error}>
                <FormLabel textColor="green.50">Email Address</FormLabel>
                <Input
                  id = "email-input-field"
                  type="email"
                  value={email}
                  placeholder="Email Address"
                  onChange={(e) => setEmail(e.target.value)}
                />
            <FormLabel textColor="green.50" mt={2}>Password</FormLabel>
                <Input
                  id = "payment-accept-status"
                  type="password"
                  placeholder="None"
                  value={paymentStatus}
                  onChange={(e) => setPaymentStatus(e.target.value)}
                />
                <FormErrorMessage>{error}</FormErrorMessage>
              </FormControl>
            <Button
              colorScheme="green.400"
              textColor="green.50"
              bg="green.400"
              size="md"
              mt={4}
              onClick={updatePaymentStatus}
              width="100%"
            >
              Update Payment Status
            </Button>
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
  
  export default PaymentAcceptance;
                