import joblib
import numpy as np
from pathlib import Path
from skl2onnx import convert_sklearn
from skl2onnx.common.data_types import FloatTensorType


def convert_to_onnx():
    model_path = Path("models/tap_classifier.pkl")
    scaler_path = Path("models/scaler.pkl")

    if not model_path.exists() or not scaler_path.exists():
        print("Error: Model files not found. Train first with: python train_tap_model.py")
        return

    print("Loading model and scaler...")
    model = joblib.load(model_path)
    scaler = joblib.load(scaler_path)

    print(f"Model classes: {model.classes_}")
    print(f"Number of features: {model.n_features_in_}")

    print("\nConverting model to ONNX...")
    initial_type = [('float_input', FloatTensorType([None, model.n_features_in_]))]

    onnx_model = convert_sklearn(
        model,
        initial_types=initial_type,
        target_opset=12
    )

    output_path = Path("models/tap_classifier.onnx")
    with open(output_path, "wb") as f:
        f.write(onnx_model.SerializeToString())

    print(f"✓ Model converted to: {output_path}")

    print("\nConverting scaler to ONNX...")
    scaler_onnx = convert_sklearn(
        scaler,
        initial_types=initial_type,
        target_opset=12
    )

    scaler_output_path = Path("models/scaler.onnx")
    with open(scaler_output_path, "wb") as f:
        f.write(scaler_onnx.SerializeToString())

    print(f"✓ Scaler converted to: {scaler_output_path}")

    print("\nExporting model metadata...")
    metadata = {
        'classes': model.classes_.tolist(),
        'n_features': int(model.n_features_in_),
        'scaler_mean': scaler.mean_.tolist(),
        'scaler_scale': scaler.scale_.tolist()
    }

    import json
    metadata_path = Path("models/model_metadata.json")
    with open(metadata_path, "w") as f:
        json.dump(metadata, f, indent=2)

    print(f"✓ Metadata exported to: {metadata_path}")

    print("\n" + "=" * 60)
    print("Conversion complete!")
    print("=" * 60)
    print("\nNext steps:")
    print("  1. Copy models/*.onnx to composeApp/src/androidMain/assets/models/")
    print("  2. Copy models/model_metadata.json to composeApp/src/androidMain/assets/models/")
    print("  3. Use ONNX Runtime in your Kotlin code to load and run inference")


if __name__ == "__main__":
    convert_to_onnx()
