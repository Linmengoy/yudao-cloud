"use client";

import { Suspense } from "react";
import { useEffect } from "react";
import { useRouter, useSearchParams } from "next/navigation";

function LegacyImageCreateRedirect() {
  const router = useRouter();
  const searchParams = useSearchParams();

  useEffect(() => {
    const query = searchParams.toString();
    router.replace(query ? `/canvas?${query}` : "/canvas");
  }, [router, searchParams]);

  return null;
}

export default function LegacyImageCreatePage() {
  return (
    <Suspense fallback={null}>
      <LegacyImageCreateRedirect />
    </Suspense>
  );
}
