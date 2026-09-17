import Link from "next/link";

export function PlaceSearchTrigger({ href, label }: { href: string; label: string }) {
  return (
    <Link
      href={href}
      className="block w-full rounded-xl border border-gray-200 bg-gray-100 px-3.5 py-3 text-left text-[14.5px] text-gray-500"
    >
      🔍 {label}
    </Link>
  );
}
