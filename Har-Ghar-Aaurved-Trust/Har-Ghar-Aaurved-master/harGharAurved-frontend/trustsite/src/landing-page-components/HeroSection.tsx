import {
    Button,
    Center,
    Container,
    Heading,
    Text,
    VStack,
  } from "@chakra-ui/react";
  import { FunctionComponent } from "react";

  interface HeroSectionProps {}

  export const HeroSection: FunctionComponent<HeroSectionProps> = () => {
    return (
      <Container maxW="container.lg">
        <Center p={4} minHeight="70vh">
          <VStack>
            <Container maxW="container.md" textAlign="center">
              <Heading size="2xl" mb={4} fontWeight={600}>
                How long will you stay still and wait for others to fix things for you
              </Heading>
              <Text fontSize="xl" textColor="gray.500">
               Peoples use trust offering to help needees and promote equality through a single click
              </Text>
              <Button
                mt={8}
                colorScheme="green"
                textColor="white"
                bgColor="green"
                textAlign={["center", "left"]}
                onClick={() => {
                  window.open("http://localhost:5173/updatePaymentStatus");
                }}
              >
                I need this for Rs 2100/year→
              </Button>
              <Text my={2} fontSize="sm" textColor="gray.400">
              1k+ peoples have already joined the trust and getting benefits from the trust
              </Text>
            </Container>
          </VStack>
        </Center>
      </Container>
    );
  };