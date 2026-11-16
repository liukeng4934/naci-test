const AVATAR_COLORS = [
  "#dbe8bd",
  "#f1cfc3",
  "#d8e0ec",
  "#efe1b7",
  "#dfd3e6",
  "#c9e2dc",
  "#ecd6ba",
  "#d3dfc9",
];

export function avatarStyle(name) {
  let hash = 0;
  for (const character of name) {
    hash = (hash * 31 + character.codePointAt(0)) % 999_983;
  }
  return { "--avatar": AVATAR_COLORS[hash % AVATAR_COLORS.length] };
}

export function avatarInitial(name) {
  const trimmed = name.trim();
  if (!trimmed) return "访";
  return Array.from(trimmed)[0].toUpperCase();
}
