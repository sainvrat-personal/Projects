import React from "react";
import "./Modal.css";

interface IModalProps {
  isOpen: boolean;
  onClose: () => void;
  title: string;
  children: React.ReactNode;
}

const Modal: React.FC<IModalProps> = ({ isOpen, onClose, title, children }) => {
  if (!isOpen) return null;
  return (
    <div className="modal-overlay">
      <div className="modal">
        <button onClick={onClose} className="modal-close" aria-label="Close">
          ×
        </button>
        <h3 className="modal-title">{title}</h3>
        {children}
      </div>
    </div>
  );
};

export default Modal;
