import joblib
import numpy as np
from pathlib import Path
import json
import shutil


def export_for_kotlin():
    model_path = Path("models/tap_classifier.pkl")
    scaler_path = Path("models/scaler.pkl")

    if not model_path.exists() or not scaler_path.exists():
        print("Error: Model files not found. Train first with: python train_tap_model.py")
        return

    print("Loading model and scaler...")
    model = joblib.load(model_path)
    scaler = joblib.load(scaler_path)

    print(f"Model type: {type(model)}")
    print(f"Model classes: {model.classes_}")
    print(f"Number of features: {model.n_features_in_}")
    print(f"Number of trees: {len(model.estimators_)}")

    trees_data = []
    for i, tree in enumerate(model.estimators_):
        tree_struct = tree.tree_

        tree_data = {
            'children_left': tree_struct.children_left.tolist(),
            'children_right': tree_struct.children_right.tolist(),
            'feature': tree_struct.feature.tolist(),
            'threshold': tree_struct.threshold.tolist(),
            'value': tree_struct.value.tolist(),
            'n_node_samples': tree_struct.n_node_samples.tolist()
        }
        trees_data.append(tree_data)

    model_data = {
        'model_type': 'RandomForest',
        'classes': model.classes_.tolist(),
        'n_features': int(model.n_features_in_),
        'n_classes': len(model.classes_),
        'n_trees': len(model.estimators_),
        'trees': trees_data,
        'scaler': {
            'mean': scaler.mean_.tolist(),
            'scale': scaler.scale_.tolist()
        }
    }

    output_path = Path("models/tap_classifier_kotlin.json")
    with open(output_path, "w") as f:
        json.dump(model_data, f, indent=2)

    print(f"\n✓ Model exported to: {output_path}")
    print(f"  File size: {output_path.stat().st_size / 1024:.1f} KB")

    compose_resources_path = Path("../composeApp/src/commonMain/composeResources/files")
    if compose_resources_path.exists():
        target_path = compose_resources_path / "tap_classifier_kotlin.json"
        shutil.copy(output_path, target_path)
        print(f"\n✓ Model copied to: {target_path}")
    else:
        print(f"\n⚠ Could not find compose resources directory at {compose_resources_path}")
        print("  Please manually copy the model file.")

    print("\n" + "=" * 60)
    print("Export complete!")
    print("=" * 60)
    print("\nNext steps:")
    print("  1. Rebuild your app to include the updated model")
    print("  2. The ML-powered tap detector will automatically use it")
    print("  3. Test the detection and adjust confidence thresholds if needed")


if __name__ == "__main__":
    export_for_kotlin()
