import React, { useEffect, useState } from "react";
import CaloriesBarChart from "../ui/CaloriesBarChart";
import { FaEdit, FaTrash } from "react-icons/fa";
import Modal from "../ui/Modal";
import Button from "../ui/Button";
import Table from "../ui/Table";
import MealForm from "./MealForm";
import { useDialog } from "../hooks/useDialog";
import { useCalorieCrud } from "../hooks/useCalorieCrud";
import { useImageAnalysis } from "../hooks/useImageAnalysis";
import "./StatsPage.css";

function getFilledDailyData(
  timeframe: "week" | "twoweek" | "fourweek",
  dailyCalories: { date: string; totalCalories: number }[]
) {
  const days = timeframe === "week" ? 7 : timeframe === "twoweek" ? 14 : 28;
  const now = new Date();
  now.setHours(0, 0, 0, 0);
  const filled: { date: string; totalCalories: number }[] = [];
  for (let i = days - 1; i >= 0; i--) {
    const d = new Date(now);
    d.setDate(now.getDate() - i);
    d.setHours(0, 0, 0, 0);
    const year = d.getFullYear();
    const month = String(d.getMonth() + 1).padStart(2, '0');
    const day = String(d.getDate()).padStart(2, '0');
    const localDateStr = `${year}-${month}-${day}`;
    const found = dailyCalories.find((dc) => dc.date === localDateStr);
    filled.push({
      date: localDateStr,
      totalCalories: found ? found.totalCalories : 0,
    });
  }
  return filled;
}

const StatsPage: React.FC = () => {
  const token = sessionStorage.getItem("accessToken") || "";
  const [timeframe, setTimeframe] = useState<"week" | "twoweek" | "fourweek">(
    "week"
  );
  const [desc, setDesc] = useState("");
  const [calories, setCalories] = useState<number>(0);
  const [editId, setEditId] = useState<number | null>(null);
  const [editDesc, setEditDesc] = useState("");
  const [editCalories, setEditCalories] = useState<number>(0);

  const addDialog = useDialog(false);
  const editDialog = useDialog(false);
  const { analyzing, analyzeImageCalorie } = useImageAnalysis();
  const { entries, loading, dailyCalories, fetchEntries, addEntry, editEntry, deleteEntry } = useCalorieCrud(
    token,
    timeframe
  );

  useEffect(() => {
    fetchEntries();
    // Only run when token or timeframe changes to avoid infinite loop
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [token, timeframe]);



  const handleAdd = async (e: React.FormEvent) => {
    e.preventDefault();
    await addEntry({ description: desc, calories });
    setDesc("");
    setCalories(0);
    addDialog.closeDialog();
    fetchEntries();
  };

  const handleImageChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0] || null;
    if (file) {
      try {
        const result = await analyzeImageCalorie(file);
        setDesc(result.description);
        setCalories(result.estimatedCalories);
      } catch (error: unknown) {
        console.error("Image analysis failed:", error);
        alert(
          error instanceof Error ? error.message : "Image analysis failed."
        );
      }
    }
  };

  const handleEdit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (editId == null) return;
    await editEntry(editId, { description: editDesc, calories: editCalories });
    editDialog.closeDialog();
    setEditId(null);
    fetchEntries();
  };

  const handleDelete = async (id: number) => {
    await deleteEntry(id);
    fetchEntries();
  };

  const filledDaily = getFilledDailyData(timeframe, dailyCalories);
  // Get user's local today in yyyy-mm-dd
  const localToday = new Date();
  localToday.setHours(0, 0, 0, 0);
  const localTodayStr = localToday.getFullYear() + '-' + String(localToday.getMonth() + 1).padStart(2, '0') + '-' + String(localToday.getDate()).padStart(2, '0');
  const todayIndex = filledDaily.findIndex((d) => d.date === localTodayStr);
  const barColors = filledDaily.map((_, idx) =>
    idx === todayIndex ? "#0ea5e9" : "rgba(16, 185, 129, 0.7)"
  );
  const dailyChartData = {
    labels: filledDaily.map((d) => new Date(d.date).toLocaleDateString()),
    datasets: [
      {
        label: "Total Calories per Day",
        data: filledDaily.map((d) => d.totalCalories),
        backgroundColor: barColors,
        borderColor: "#10b981",
        borderWidth: 1,
      },
    ],
  };

  return (
    <div className="stats-container">
      <h2 className="stats-title">Your Calorie Stats</h2>
      <div className="stats-flex-col">
        <div
          className="stats-flex-row"
          style={{ justifyContent: "space-between" }}
        >
          <div>
            <Button onClick={addDialog.openDialog} className="add-meal-btn">
              + Add Meal
            </Button>
          </div>
          <div className="stats-flex-row-inner">
            <div className="timeframe-btn-group">
              <Button
                variant={timeframe === "week" ? "primary" : "secondary"}
                onClick={() => setTimeframe("week")}
                className={`timeframe-btn${
                  timeframe === "week" ? " active" : ""
                }`}
              >
                1W
              </Button>
              <Button
                variant={timeframe === "twoweek" ? "primary" : "secondary"}
                onClick={() => setTimeframe("twoweek")}
                className={`timeframe-btn${
                  timeframe === "twoweek" ? " active" : ""
                }`}
              >
                2W
              </Button>
              <Button
                variant={timeframe === "fourweek" ? "primary" : "secondary"}
                onClick={() => setTimeframe("fourweek")}
                className={`timeframe-btn${
                  timeframe === "fourweek" ? " active" : ""
                }`}
              >
                4W
              </Button>
            </div>
          </div>
        </div>
        <div className="stats-chart-container" style={{ width: "100%" }}>
          <CaloriesBarChart data={dailyChartData} height={350} />
        </div>
      </div>

      <Modal
        isOpen={addDialog.open}
        onClose={addDialog.closeDialog}
        title="Add Meal"
      >
        <MealForm
          description={desc}
          calories={calories}
          onDescriptionChange={setDesc}
          onCaloriesChange={setCalories}
          onImageChange={handleImageChange}
          onSubmit={handleAdd}
          loading={loading}
          analyzing={analyzing}
          showImageInput
          submitLabel="Add Meal"
        />
      </Modal>

      <Modal
        isOpen={editDialog.open}
        onClose={editDialog.closeDialog}
        title="Edit Meal"
      >
        <MealForm
          description={editDesc}
          calories={editCalories}
          onDescriptionChange={setEditDesc}
          onCaloriesChange={setEditCalories}
          onSubmit={handleEdit}
          submitLabel="Save"
        />
      </Modal>

      <div style={{ width: "100%" }}>
        <Table
          columns={[
            {
              key: "createdAt",
              label: "Date",
              render: (row) => {
                const d = new Date(row.createdAt);
                const year = d.getFullYear();
                const month = String(d.getMonth() + 1).padStart(2, '0');
                const day = String(d.getDate()).padStart(2, '0');
                const hour = String(d.getHours()).padStart(2, '0');
                const minute = String(d.getMinutes()).padStart(2, '0');
                return `${year}-${month}-${day} ${hour}:${minute}`;
              },
            },
            { key: "description", label: "Description" },
            { key: "calories", label: "Calories" },
          ]}
          data={entries}
          actions={(entry) => (
            <>
              <Button
                title="Edit"
                variant="secondary"
                className="stats-edit-btn"
                onClick={() => {
                  setEditId(entry.id);
                  setEditDesc(entry.description);
                  setEditCalories(entry.calories);
                  editDialog.openDialog();
                }}
              >
                <FaEdit />
              </Button>
              <Button
                title="Delete"
                variant="danger"
                className="stats-delete-btn"
                onClick={() => handleDelete(entry.id)}
              >
                <FaTrash />
              </Button>
            </>
          )}
        />
      </div>
    </div>
  );
};

export default StatsPage;
