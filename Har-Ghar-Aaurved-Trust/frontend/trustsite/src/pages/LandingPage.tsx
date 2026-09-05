import { HeroSection } from "../landing-page-components/HeroSection";
import { Layout } from "../landing-page-components/Layout";
import { Feature } from "../landing-page-components/Feature";
import { PricingSection } from "../landing-page-components/PricingSection";
import { FAQSection, FAQType } from "../landing-page-components/FAQSection";
import { CTA } from '../landing-page-components/CTA';
import {
  Container,
  Box,
  Text,
  Image,
  VStack,
  SimpleGrid,
  Flex,
  LinkBox,
  LinkOverlay,
  Spacer,
} from "@chakra-ui/react";
import {Helmet} from "react-helmet";
//import { SetStateAction, useState } from 'react';
//import Login from "../auth/Login";

interface FeatureType {
  title: string
  description: string
  image: string
}
const features: FeatureType[] = [
  {
    title: "Promotion of Naturopathy and Alternative Medicines",
    description:
      "(i)	To conduct awareness campaigns, workshops, and seminars on the principles and benefits of naturopathy and alternative medicine.",
    image:
      "https://launchman-space.nyc3.digitaloceanspaces.com/chakra-ui-landing-page-feature-1.png",
  },
  {
    title: "Cultivation and Utilization of Medicinal Plants",
    description:
      "(i)	To educate farmers about the cultivation, harvesting, and sustainable practices of medicinal plants.",
    image:
      "https://launchman-space.nyc3.digitaloceanspaces.com/chakra-ui-landing-page-feature-2.png",
  },
  {
    title: "Awareness and Education",
    description:
      "(i)	To create educational materials, publications, and online resources to disseminate information about the benefits of medicinal plants and natural therapies. (ii)	To organize community outreach programs in rural and urban areas to raise awareness about the importance of natural wellness practices. (iii)	To collaborate with schools and educational institutions to integrate curriculum elements related to naturopathy, alternative medicine, and the importance of medicinal plants. (iv)	Further to develop schools ,colleges and centres of excellence to promote naturopathy, alternative medicine, non invasive therapies, and eco-friendly products.",
    image:
      "https://launchman-space.nyc3.digitaloceanspaces.com/chakra-ui-landing-page-feature-3.png",
  },
];

const faqs: FAQType[] = [
  {
    q: 'What are the benefits we get after becoming trustee?',
    a: 'You will get multiple benefites like discuont in treatment, learning-session on Naturography and medical plants, you will get opporuntities to meet in person to the needees. etc.',
  },
  {
    q: 'Where is the headQuarter for the trust?',
    a: 'RightNow.. it is based on Hisar, Haryana, India.',
  },
  {
    q: 'Do we accept international payements?',
    a: 'No - Rightnow we are focusing to provide our services with-in India only. Even though any kind of financial support is more than welcomed!',
  },
  {
    q: 'Whom can I connect to for support?',
    a: 'Email me at pradeepAmbedkar@gamil.com',
  },
]

export interface HighlightType {
  icon: string
  title: string
  description: string
}
const highlights: HighlightType[] = [
      {
        icon: '✨',
        title: 'No-code',
        description:
          "We are No-Code friendly. There is no coding required to get started. Launchman connects with Airtable and lets you generate a new page per row. It's just that easy!",
      },
      {
        icon: '🎉',
        title: 'Make Google happy',
        description:
          "We render all our pages server-side; when Google's robots come to index your site, the page does not have to wait for JS to be fetched. This helps you get ranked higher.",
      },
      {
        icon: '😃',
        title: 'Rapid experimenting',
        description:
          "You don't have to wait hours to update your hard-coded landing pages. Figure out what resonates with your customers the most and update the copy in seconds",
      },
      {
        icon: '🔌',
        title: 'Rapid experimenting',
        description:
          "You don't have to wait hours to update your hard-coded landing pages. Figure out what resonates with your customers the most and update the copy in seconds",
      },
    ]
    //
export const LandingPage = () => {
  return (
    <Layout isLoggedIn={false} handleLogout={false}> 
      <Helmet>
        <meta charSet="utf-8" />
        <title>HAR GHAR AUSHADHI FOUNDATION CHARITABLE TRUST</title>
      </Helmet>
      <Box bg="gray.50">
        <HeroSection />
          <VStack
          backgroundColor="white"
          w="full"
          id="features"
          spacing={16}
          py={[16, 0]}
        >
          {features.map(
            ({ title, description, image }: FeatureType, i: number) => {
              return (
                <Feature
                  key={`feature_${i}`}
                  title={title}
                  description={description}
                  image={image}
                  reverse={i % 2 === 1}
                  reverseColor={i % 2 === 0 }
                />
              )
            }
          )}
        </VStack>
        <Container maxW="container.md" centerContent py={[8, 28]}>
            <SimpleGrid spacingX={10} spacingY={20} minChildWidth="300px">
              {highlights.map(({ title, description, icon }, i: number) => (
                <Box p={4} rounded="md" key={`highlight_${i}`}>
                  <Text fontSize="4xl">{icon}</Text>
                  <Text fontWeight={500}>{title}</Text>
                  <Text color="gray.500" mt={4}>
                    {description}
                  </Text>
                </Box>
              ))}
            </SimpleGrid>
          </Container>
        <Container py={28} maxW="container.lg" w="full" id="pricing">
          <PricingSection />
        </Container>
        <Container py={28} maxW="container.md">
          <Box w="full">
            <VStack spacing={10} w="full">
              <Text fontWeight={500} fontSize="2xl" align="center">
                Frequently asked questions
              </Text>
              <FAQSection items={faqs} />
            </VStack>
          </Box>
        </Container>
        <CTA heading={`Join trust today!`} cta={{ name: 'I want to join!', link: '#updatePaymentStatus' }}/>
        <Container maxW="container.2xl" bgColor="green.50">
          <Flex py={6}>
            <Box>
              <Text>© 2024 Pardeep Ambedkar</Text>
              <Text>Made by Sainvrat & Akash Mundara</Text>
            </Box>
            <Spacer />
            <LinkBox>
              <LinkOverlay href="https://twitter.com/@thisissukh_" isExternal>
                <Image src="twitter.svg" alt="Twitter logo"></Image>
              </LinkOverlay>
            </LinkBox>
          </Flex>
        </Container>
      </Box>
    </Layout>
  );
};

/**
 * <Container maxW="container.xl">
          <Center p={[0, 10]}>
            <video playsInline autoPlay muted poster="/image.png" loop>
              <source src="/video.mp4" type="video/mp4" />
            </video>
          </Center>
		</Container>
    <Container maxW="container.2xl" centerContent py={[20]}>
            <Text color="gray.600" fontSize="lg">
              Nourishing Society Through Naturography and Medical Research
            </Text>
            <Wrap
              spacing={[10, 20]}
              mt={8}
              align="center"
              justify="center"
              w="full"
            >
              <WrapItem>
                <Image src="microsoft-logo.svg" alt="Microsoft logo" />
              </WrapItem>
              <WrapItem>
                <Image src="adobe-logo.svg" alt="Adobe logo" />
              </WrapItem>
              <WrapItem>
                <Image src="microsoft-logo.svg" alt="Microsoft logo" />
              </WrapItem>
              <WrapItem>
                <Image src="adobe-logo.svg" alt="Adobe logo" />
              </WrapItem>
            </Wrap>
          </Container> 
 */