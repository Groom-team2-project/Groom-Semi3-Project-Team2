import Link from "next/link";

export function AppBar({
  title,
  subtitle,
  backHref,
  actions,
}: {
  title: React.ReactNode;
  subtitle?: React.ReactNode;
  backHref?: string;
  actions?: React.ReactNode;
}) {
  return (
    <header className="sticky top-0 z-10 flex flex-shrink-0 items-center gap-2.5 bg-white px-4 pb-2.5 pt-4">
      {backHref && (
        <Link
          href={backHref}
          aria-label="뒤로"
          className="-ml-1 flex h-8 w-8 items-center justify-center text-[19px] text-gray-700"
        >
          ‹
        </Link>
      )}
      <div className="min-w-0">
        <div className="truncate text-[17px] font-extrabold tracking-tight">{title}</div>
        {subtitle && <div className="truncate text-[11.5px] text-gray-500">{subtitle}</div>}
      </div>
      <div className="flex-1" />
      {actions && <div className="flex items-center gap-1.5">{actions}</div>}
    </header>
  );
}
