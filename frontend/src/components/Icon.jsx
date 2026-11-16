const paths = {
  arrowRight: <><path d="M4 10h12M11.5 5.5 16 10l-4.5 4.5" /></>,
  clock: <><circle cx="10" cy="10" r="7.1" /><path d="M10 5.8V10l2.8 1.8" /></>,
  heart: <><path d="M10 16.6S3.3 12.8 3.3 7.9A3.5 3.5 0 0 1 10 6.1a3.5 3.5 0 0 1 6.7 1.8c0 4.9-6.7 8.7-6.7 8.7Z" /></>,
  reply: <><path d="M8.3 5 3.7 9.1l4.6 4" /><path d="M4.2 9.1h6.2c3.2 0 5.3 1.8 5.3 5" /></>,
  search: <><circle cx="8.7" cy="8.7" r="5.7" /><path d="m13 13 3.5 3.5" /></>,
  close: <><path d="m5 5 10 10M15 5 5 15" /></>,
  lock: <><rect x="4.5" y="8.5" width="11" height="8" rx="2" /><path d="M7 8.5V6.6a3 3 0 0 1 6 0v1.9" /></>,
  chat: <><path d="M3.5 4.5h13v9h-8l-4 3v-3h-1z" /><path d="M7 8h6M7 11h4" /></>,
  cloud: <><path d="M5.8 15.5h8.5a3.4 3.4 0 0 0 .4-6.8 5.1 5.1 0 0 0-9.7-1.4 4.1 4.1 0 0 0 .8 8.2Z" /><path d="M8 17.5h5" /></>,
  sparkle: <><path d="M10 2.8c.5 4.1 2.7 6.3 6.8 6.8-4.1.5-6.3 2.7-6.8 6.8-.5-4.1-2.7-6.3-6.8-6.8 4.1-.5 6.3-2.7 6.8-6.8Z" /></>,
  user: <><circle cx="10" cy="6.4" r="3.1" /><path d="M4.3 16.3c.5-3 2.7-4.8 5.7-4.8s5.2 1.8 5.7 4.8" /></>,
  logout: <><path d="M8.6 4.2H4.8v11.6h3.8" /><path d="M8.4 10h8.1M13.7 6.7 17 10l-3.3 3.3" /></>,
  trash: <><path d="M4.2 6.2h11.6M8 6.2V4.5h4v1.7M5.8 6.2l.7 9.2h7l.7-9.2M8.3 8.8v4M11.7 8.8v4" /></>,
  chevronDown: <><path d="m6 8 4 4 4-4" /></>,
};

export function Icon({ name, className, ...props }) {
  return (
    <svg
      className={className}
      viewBox="0 0 20 20"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.45"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      {...props}
    >
      {paths[name]}
    </svg>
  );
}
