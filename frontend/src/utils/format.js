const fullDateFormatter = new Intl.DateTimeFormat("zh-CN", {
  year: "numeric",
  month: "2-digit",
  day: "2-digit",
  hour: "2-digit",
  minute: "2-digit",
  hour12: false,
});

const shortDateFormatter = new Intl.DateTimeFormat("zh-CN", {
  month: "2-digit",
  day: "2-digit",
  hour: "2-digit",
  minute: "2-digit",
  hour12: false,
});

export function fullDate(isoString) {
  return fullDateFormatter.format(new Date(isoString));
}

export function relativeTime(isoString) {
  const time = new Date(isoString).getTime();
  if (Number.isNaN(time)) return "";

  const difference = Date.now() - time;
  if (difference < 60_000) return "刚刚";

  const minutes = Math.floor(difference / 60_000);
  if (minutes < 60) return `${minutes} 分钟前`;

  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours} 小时前`;

  const days = Math.floor(hours / 24);
  if (days < 7) return `${days} 天前`;

  return shortDateFormatter.format(new Date(isoString));
}

export function isToday(isoString) {
  const date = new Date(isoString);
  const today = new Date();
  return (
    date.getFullYear() === today.getFullYear() &&
    date.getMonth() === today.getMonth() &&
    date.getDate() === today.getDate()
  );
}
