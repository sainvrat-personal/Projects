import { Box, VStack } from "@chakra-ui/react";
import { FunctionComponent } from "react";
import { Header } from "./Header";
interface LayoutProps {
  children: React.ReactNode;
  isLoggedIn : boolean;
  handleLogout : any;
}
export const Layout: FunctionComponent<LayoutProps> = ({
  children, isLoggedIn, handleLogout
}: LayoutProps) => {
  return (
    <Box bg="green.50">
      <VStack spacing={10} w="full" align="center">
        <Header name="Har Ghar Aushdhi Foundation Charitable Trust" isLoggedIn={isLoggedIn} handleLogout={handleLogout} />
        {children}
      </VStack>
    </Box>
  );
};