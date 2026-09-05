import {
    Box,
    Button,
    Center,
    Container,
    Stack,
    Text,
    VStack,
    Image,
  } from "@chakra-ui/react";
  import { FunctionComponent } from "react";
  interface FeatureProps {
    title: string;
    description: string;
    image: string;
    reverse?: boolean;
    reverseColor?: boolean;
  }
  export const Feature: FunctionComponent<FeatureProps> = ({
    title,
    description,
    image,
    reverse,
    reverseColor
  }: FeatureProps) => {
    const rowDirection = reverse ? "row-reverse" : "row";
    const bgColor = reverseColor ? "white" : "gray.50";
    return (
      <Center w="full" minH={[null, "90vh"]} bg={bgColor}>
        <Container maxW="container.xl" rounded="lg">
          <Stack
            spacing={[4, 16]}
            alignItems="center"
            direction={["column", null, rowDirection]}
            w="full"
            h="full"
          >
            <Box rounded="lg">
              <Image
                src={image}
                width={684}
                height={433}
                alt={`Feature: ${title}`}
              />
            </Box>
            <VStack maxW={500} spacing={4} align={["center", "flex-start"]}>
              <Box>
                <Text fontSize="3xl" fontWeight={600} align={["center", "left"]}>
                  {title}
                </Text>
              </Box>
              <Text fontSize="md" color="gray.500" textAlign={["center", "left"]}>
                {description}
              </Text>
              <Button
                colorScheme="green"
                variant="link"
                textAlign={["center", "left"]}
              >
                Learn more →
              </Button>
            </VStack>
          </Stack>
        </Container>
      </Center>
    );
  };
  