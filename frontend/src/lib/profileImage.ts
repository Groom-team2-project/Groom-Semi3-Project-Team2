export const PROFILE_IMAGE_MAX_BYTES = 10 * 1024 * 1024;
export const PROFILE_IMAGE_MAX_SIDE = 1024;
export const PROFILE_IMAGE_TYPES = ["image/jpeg", "image/png", "image/webp"] as const;

export interface ProcessedProfileImage {
  blob: Blob;
  originalWidth: number;
  originalHeight: number;
  width: number;
  height: number;
}

export function validateProfileImage(file: File): string | null {
  if (!PROFILE_IMAGE_TYPES.includes(file.type as (typeof PROFILE_IMAGE_TYPES)[number])) {
    return "JPEG, PNG, WebP 이미지만 선택할 수 있어요.";
  }
  if (file.size > PROFILE_IMAGE_MAX_BYTES) {
    return "이미지 크기는 10MB 이하여야 해요.";
  }
  return null;
}

function loadImage(source: string): Promise<HTMLImageElement> {
  return new Promise((resolve, reject) => {
    const image = new Image();
    image.onload = () => resolve(image);
    image.onerror = () => reject(new Error("이미지 파일을 읽을 수 없어요."));
    image.src = source;
  });
}

export async function processProfileImage(file: File): Promise<ProcessedProfileImage> {
  const validationError = validateProfileImage(file);
  if (validationError) throw new Error(validationError);

  const source = URL.createObjectURL(file);
  try {
    const image = await loadImage(source);
    const scale = Math.min(
      1,
      PROFILE_IMAGE_MAX_SIDE / image.naturalWidth,
      PROFILE_IMAGE_MAX_SIDE / image.naturalHeight,
    );
    const width = Math.max(1, Math.round(image.naturalWidth * scale));
    const height = Math.max(1, Math.round(image.naturalHeight * scale));
    const canvas = document.createElement("canvas");
    canvas.width = width;
    canvas.height = height;
    const context = canvas.getContext("2d");
    if (!context) throw new Error("이 브라우저에서는 이미지 변환을 지원하지 않아요.");
    context.fillStyle = "#FFFFFF";
    context.fillRect(0, 0, width, height);
    context.drawImage(image, 0, 0, width, height);

    const blob = await new Promise<Blob | null>((resolve) => {
      canvas.toBlob(resolve, "image/jpeg", 0.85);
    });
    if (!blob) throw new Error("이미지를 변환하지 못했어요.");

    return {
      blob,
      originalWidth: image.naturalWidth,
      originalHeight: image.naturalHeight,
      width,
      height,
    };
  } finally {
    URL.revokeObjectURL(source);
  }
}

export function formatImageBytes(bytes: number): string {
  return bytes < 1024 * 1024
    ? `${Math.max(1, Math.round(bytes / 1024))}KB`
    : `${(bytes / 1024 / 1024).toFixed(1)}MB`;
}
