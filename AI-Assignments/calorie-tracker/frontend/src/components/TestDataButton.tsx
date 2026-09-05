import React, { useState } from "react";
import Modal from "../ui/Modal";
import Button from "../ui/Button";
import { useTestDataApi } from "../hooks/useTestDataApi";

interface TestDataButtonProps {
  token: string;
  onTestDataAdded?: () => void;
}

const TestDataButton: React.FC<TestDataButtonProps> = ({
  token,
  onTestDataAdded,
}) => {
  const { generateTestData } = useTestDataApi(token);
  const [testDataDialog, setTestDataDialog] = useState(false);
  const [testDataLoading, setTestDataLoading] = useState(false);
  const [testDataResult, setTestDataResult] = useState<string | null>(null);

  const handleGenerateTestData = async () => {
    setTestDataLoading(true);
    setTestDataResult(null);
    try {
      const res = await generateTestData();
      setTestDataResult(
        res.success ? "Test data generated!" : res.error || "Failed"
      );
      if (res.success && onTestDataAdded) onTestDataAdded();
    } catch (e: unknown) {
      setTestDataResult(e instanceof Error ? e.message : "Failed");
    } finally {
      setTestDataLoading(false);
    }
  };

  return (
    <>
      <Button variant="secondary" onClick={() => setTestDataDialog(true)}>
        Add Test Data
      </Button>
      <Modal
        isOpen={testDataDialog}
        onClose={() => setTestDataDialog(false)}
        title="Add Test Data"
      >
        <div style={{ marginBottom: 16 }}>
          <p>
            This will add mock calorie entries for the last 30 days for your
            user. This is for testing graphs and tables.
            <br />
            <b>This action cannot be undone.</b>
          </p>
          {testDataResult && (
            <div
              style={{
                color: testDataResult.includes("success") ? "green" : "red",
              }}
            >
              {testDataResult}
            </div>
          )}
        </div>
        <Button
          variant="primary"
          onClick={handleGenerateTestData}
          disabled={testDataLoading}
        >
          {testDataLoading ? "Generating..." : "Yes, Add Test Data"}
        </Button>
        <Button
          variant="secondary"
          onClick={() => setTestDataDialog(false)}
          style={{ marginLeft: 8 }}
        >
          Cancel
        </Button>
      </Modal>
    </>
  );
};

export default TestDataButton;
