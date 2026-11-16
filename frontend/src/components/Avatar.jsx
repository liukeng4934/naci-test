import { avatarInitial, avatarStyle } from "../utils/avatar.js";

export function Avatar({ name, small = false }) {
  return (
    <span
      className={`avatar${small ? " avatar-small" : ""}`}
      style={avatarStyle(name)}
      aria-hidden="true"
    >
      {avatarInitial(name)}
    </span>
  );
}
