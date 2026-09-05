import {
  Box,
  chakra,
  Container,
  Flex,
  Heading,
  IconButton,
  Link,
  Spacer,
  Stack,
  useDisclosure,
  LinkOverlay,
  LinkBox,
  Image,
  Drawer,
  DrawerOverlay,
  DrawerContent,
  DrawerCloseButton,
  DrawerHeader,
  DrawerBody
} from '@chakra-ui/react';
import { HamburgerIcon } from '@chakra-ui/icons';

const navLinks = [
  { name: 'Features', link: '#features' },
  { name: 'Pricing', link: '#pricing' },
  { name: 'Campaigns', link: '#campaigns' },
  { name: 'Gallery', link: '#gallery' },
  { name: 'Details', link: '#details' },
  { name: 'Payments', link: 'updatePaymentStatus' }
];

const DesktopSidebarContents = ({ name, isLoggedIn, handleLogout }: any) => {
  return (
    <Container maxW="container.2xl" p={0} backgroundColor="green.500" textColor="green.50">
      <Flex justify="space-between" alignItems="center" p={4}>
        <Flex alignItems="center">
          <LinkBox>
            <LinkOverlay href={"/"} isExternal>
              <Image src="../../public/favicon.ico" h={8} />
            </LinkOverlay>
          </LinkBox>
          <Heading fontSize="xl" ml={4} display={{ base: 'none', md: 'flex' }}>
            {name}
          </Heading>
        </Flex>
        <Stack direction="row" spacing={6}>
          {navLinks.map((navLink: any, i: number) => (
            <Link
              key={`navlink_${i}`}
              href={navLink.link}
              fontWeight={500}
              variant="ghost"
              textColor="green.50"
            >
              {navLink.name}
            </Link>
          ))}
        </Stack>
        <Stack direction="row" spacing={6}>
          <Link href="/login" fontWeight="500" variant="ghost" textColor="green.50">Login</Link>
          {isLoggedIn && <button onClick={handleLogout}>Logout</button>}
          <Link href="/signup" fontWeight="500" variant="ghost" textColor="green.50">Signup</Link>
        </Stack>
      </Flex>
    </Container>
  );
};

const MobileSidebar = ({ name, isLoggedIn, handleLogout }: any) => {
  const { isOpen, onOpen, onClose } = useDisclosure();

  return (
    <Flex justify="space-between" align="center" p={4}>
      <Heading fontSize="xl">{name}</Heading>
      <Spacer />
      <IconButton
        aria-label="Open navigation"
        icon={<HamburgerIcon />}
        onClick={onOpen}
      />
      <Drawer isOpen={isOpen} placement="right" onClose={onClose} size="xs">
        <DrawerOverlay />
        <DrawerContent bg="gray.50">
          <DrawerCloseButton />
          <DrawerHeader>{name}</DrawerHeader>
          <DrawerBody>
            <DesktopSidebarContents name={name} isLoggedIn={isLoggedIn} handleLogout={handleLogout} />
          </DrawerBody>
        </DrawerContent>
      </Drawer>
    </Flex>
  );
};

interface SidebarProps {
  name: string;
  isLoggedIn: boolean;
  handleLogout: any;
}

const Sidebar = ({ name, isLoggedIn, handleLogout }: SidebarProps) => {
  return (
    <chakra.header id="header">
      <Box display={{ base: 'flex', md: 'none' }} p={4}>
        <MobileSidebar name={name} isLoggedIn={isLoggedIn} handleLogout={handleLogout} />
      </Box>
      <Box display={{ base: 'none', md: 'flex' }} bg="gray.50">
        <DesktopSidebarContents name={name} isLoggedIn={isLoggedIn} handleLogout={handleLogout} />
      </Box>
    </chakra.header>
  );
};

interface HeaderProps {
  name: string;
  isLoggedIn: boolean;
  handleLogout: any;
}

export const Header = ({ name, isLoggedIn, handleLogout }: HeaderProps) => {
  return (
    <Box w="full">
      <Sidebar name={name} isLoggedIn={isLoggedIn} handleLogout={handleLogout} />
    </Box>
  );
};