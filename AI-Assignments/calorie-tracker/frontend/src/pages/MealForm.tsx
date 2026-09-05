import React from "react";
import Button from "../ui/Button";

interface IMealFormProps {
  description: string;
  calories: number;
  onDescriptionChange: (desc: string) => void;
  onCaloriesChange: (cal: number) => void;
  onImageChange?: (e: React.ChangeEvent<HTMLInputElement>) => void;
  onSubmit: (e: React.FormEvent) => void;
  loading?: boolean;
  analyzing?: boolean;
  showImageInput?: boolean;
  submitLabel?: string;
}

const MealForm: React.FC<IMealFormProps> = ({
  description,
  calories,
  onDescriptionChange,
  onCaloriesChange,
  onImageChange,
  onSubmit,
  loading,
  analyzing,
  showImageInput = false,
  submitLabel = "Save",
}) => (
  <form onSubmit={onSubmit} className="stats-form">
    <input
      required
      placeholder="Description"
      value={description}
      onChange={(e) => onDescriptionChange(e.target.value)}
      className="stats-input"
      disabled={!!analyzing}
    />
    <input
      required
      type="number"
      placeholder="Calories"
      value={calories === 0 ? "" : calories}
      onChange={(e) =>
        onCaloriesChange(
          e.target.value === "" ? 0 : Math.max(0, Number(e.target.value))
        )
      }
      className="stats-input"
      disabled={!!analyzing}
    />
    {showImageInput && (
      <>
        <label className="upload-file-label">
          Upload Image
          <input
            type="file"
            accept="image/*"
            onChange={onImageChange}
            className="upload-file-input"
            disabled={!!analyzing}
          />
        </label>
      </>
    )}
    <Button
      type="submit"
      disabled={!!loading || !!analyzing}
      className="stats-submit-btn"
    >
      {analyzing ? "Detecting..." : submitLabel}
    </Button>
  </form>
);

export default MealForm;
