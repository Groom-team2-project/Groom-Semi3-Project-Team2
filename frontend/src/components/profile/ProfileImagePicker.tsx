"use client";

import { useEffect, useRef, useState } from "react";
import { Avatar } from "@/components/ui/Avatar";
import { processProfileImage, validateProfileImage } from "@/lib/profileImage";

interface ProfileImagePickerProps {
  name: string;
  color: string;
  imageUrl?: string;
  onUpload: (image: Blob) => Promise<void>;
}

export function ProfileImagePicker({ name, color, imageUrl, onUpload }: ProfileImagePickerProps) {
  const inputRef = useRef<HTMLInputElement>(null);
  const requestRef = useRef(0);
  const [isOpen, setIsOpen] = useState(false);
  const [isUploading, setIsUploading] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => () => {
    requestRef.current += 1;
  }, []);

  function closeDialog() {
    if (isUploading) return;
    requestRef.current += 1;
    setError("");
    setIsOpen(false);
  }

  async function handleFile(file: File) {
    const request = ++requestRef.current;
    const validationError = validateProfileImage(file);
    if (validationError) {
      setError(validationError);
      return;
    }

    setError("");
    setIsUploading(true);
    try {
      const { blob } = await processProfileImage(file);
      if (request !== requestRef.current) return;
      await onUpload(blob);
      if (request === requestRef.current) setIsOpen(false);
    } catch (reason) {
      if (request === requestRef.current) {
        setError(reason instanceof Error ? reason.message : "프로필 이미지를 변경하지 못했어요.");
      }
    } finally {
      if (request === requestRef.current) setIsUploading(false);
    }
  }

  return (
    <>
      <button
        type="button"
        aria-label="프로필 이미지 변경"
        className="shrink-0 rounded-full focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-primary"
        onClick={() => setIsOpen(true)}
      >
        {imageUrl ? (
          // eslint-disable-next-line @next/next/no-img-element
          <img src={imageUrl} alt="" className="h-[52px] w-[52px] rounded-full border-2 border-white object-cover" />
        ) : (
          <Avatar name={name} color={color} size="lg" />
        )}
      </button>

      {isOpen && (
        <div
          className="fixed inset-0 z-50 flex items-end justify-center bg-ink/40 px-4 pb-6 sm:items-center sm:pb-0"
          role="presentation"
          onMouseDown={(event) => {
            if (event.target === event.currentTarget) closeDialog();
          }}
        >
          <div role="dialog" aria-modal="true" aria-labelledby="profile-image-title" className="w-full max-w-sm rounded-2xl bg-white p-5 shadow-xl">
            <h2 id="profile-image-title" className="text-[17px] font-bold text-ink">프로필 이미지 변경</h2>
            <p className="mt-1 text-[13px] leading-5 text-gray-500">사진을 선택하면 바로 프로필에 적용돼요.</p>
            <input
              ref={inputRef}
              className="sr-only"
              type="file"
              accept="image/jpeg,image/png,image/webp"
              onChange={(event) => {
                const file = event.target.files?.[0];
                event.target.value = "";
                if (file) void handleFile(file);
              }}
            />
            <button
              type="button"
              className="mt-5 w-full rounded-xl bg-primary px-4 py-3 text-[14px] font-bold text-white disabled:opacity-50"
              disabled={isUploading}
              onClick={() => inputRef.current?.click()}
            >
              {isUploading ? "적용 중…" : "사진 선택"}
            </button>
            <button type="button" className="mt-2 w-full rounded-xl px-4 py-3 text-[14px] font-bold text-gray-500 disabled:opacity-50" disabled={isUploading} onClick={closeDialog}>
              취소
            </button>
            {error && <p role="alert" className="mt-2 text-center text-[12px] text-red">{error}</p>}
          </div>
        </div>
      )}
    </>
  );
}
