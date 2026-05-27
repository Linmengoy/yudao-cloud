export type GenerationModelKind = "image" | "video" | "3d";

export type GenerationModel = {
  id: string;
  kind: GenerationModelKind;
  name: string;
  description: string;
  estimatedSeconds?: number;
  quality?: "auto" | "low" | "medium" | "high";
  enabled: boolean;
};

export const IMAGE_MODELS: GenerationModel[] = [
  {
    id: "gpt-image-2",
    kind: "image",
    name: "GPT Image 2",
    description: "Copse test provider via Images API.",
    estimatedSeconds: 30,
    quality: "auto",
    enabled: true,
  },
];

export function getModelById(id: string): GenerationModel | undefined {
  return IMAGE_MODELS.find((m) => m.id === id);
}

export const DEFAULT_MODEL_ID = "gpt-image-2";
