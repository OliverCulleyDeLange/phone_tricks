import json
import matplotlib.pyplot as plt
import numpy as np
from pathlib import Path
import sys

def load_training_data(json_file):
    with open(json_file, 'r') as f:
        return json.load(f)

def plot_sensor_data(data, output_file=None):
    label = data['label']
    tap_timestamps = data['tapTimestamps']
    accel_data = data['accelerometerData']
    gyro_data = data['gyroscopeData']
    
    accel_times = [point['timestampMs'] for point in accel_data]
    accel_x = [point['x'] for point in accel_data]
    accel_y = [point['y'] for point in accel_data]
    accel_z = [point['z'] for point in accel_data]
    
    gyro_times = [point['timestampMs'] for point in gyro_data]
    gyro_x = [point['x'] for point in gyro_data]
    gyro_y = [point['y'] for point in gyro_data]
    gyro_z = [point['z'] for point in gyro_data]
    
    start_time = min(accel_times[0], gyro_times[0])
    accel_times_rel = [(t - start_time) / 1000.0 for t in accel_times]
    gyro_times_rel = [(t - start_time) / 1000.0 for t in gyro_times]
    tap_times_rel = [(t - start_time) / 1000.0 for t in tap_timestamps]
    
    fig, (ax1, ax2) = plt.subplots(2, 1, figsize=(14, 10))
    fig.suptitle(f'Sensor Data - {label}\n({len(tap_timestamps)} taps)', fontsize=16, fontweight='bold')
    
    ax1.plot(accel_times_rel, accel_x, label='X', linewidth=1, alpha=0.8)
    ax1.plot(accel_times_rel, accel_y, label='Y', linewidth=1, alpha=0.8)
    ax1.plot(accel_times_rel, accel_z, label='Z', linewidth=1, alpha=0.8)
    
    for i, tap_time in enumerate(tap_times_rel):
        ax1.axvline(x=tap_time, color='red', linestyle='--', linewidth=2, alpha=0.7, label='Tap' if i == 0 else '')
        ax1.text(tap_time, ax1.get_ylim()[1], f'{i+1}', ha='center', va='bottom', fontsize=10, color='red', fontweight='bold')
    
    ax1.set_xlabel('Time (seconds)', fontsize=12)
    ax1.set_ylabel('Acceleration (m/s²)', fontsize=12)
    ax1.set_title('Accelerometer Data', fontsize=14, fontweight='bold')
    ax1.legend(loc='upper right')
    ax1.grid(True, alpha=0.3)
    
    ax2.plot(gyro_times_rel, gyro_x, label='X', linewidth=1, alpha=0.8)
    ax2.plot(gyro_times_rel, gyro_y, label='Y', linewidth=1, alpha=0.8)
    ax2.plot(gyro_times_rel, gyro_z, label='Z', linewidth=1, alpha=0.8)
    
    for i, tap_time in enumerate(tap_times_rel):
        ax2.axvline(x=tap_time, color='red', linestyle='--', linewidth=2, alpha=0.7, label='Tap' if i == 0 else '')
        ax2.text(tap_time, ax2.get_ylim()[1], f'{i+1}', ha='center', va='bottom', fontsize=10, color='red', fontweight='bold')
    
    ax2.set_xlabel('Time (seconds)', fontsize=12)
    ax2.set_ylabel('Angular Velocity (rad/s)', fontsize=12)
    ax2.set_title('Gyroscope Data', fontsize=14, fontweight='bold')
    ax2.legend(loc='upper right')
    ax2.grid(True, alpha=0.3)
    
    plt.tight_layout()
    
    if output_file:
        plt.savefig(output_file, dpi=150, bbox_inches='tight')
        print(f"  ✓ Saved: {output_file}")
    else:
        plt.show()
    
    plt.close(fig)

def batch_process():
    training_data_dir = Path("training_data")
    if not training_data_dir.exists():
        print("Error: training_data directory not found")
        sys.exit(1)
    
    json_files = sorted(training_data_dir.glob("*.json"))
    
    if not json_files:
        print("No JSON files found in training_data/")
        sys.exit(1)
    
    print(f"Found {len(json_files)} training data file(s)")
    print("=" * 60)
    
    visualizations_dir = Path("visualizations")
    visualizations_dir.mkdir(exist_ok=True)
    
    for json_file in json_files:
        try:
            print(f"\nProcessing: {json_file.name}")
            data = load_training_data(json_file)
            
            output_file = visualizations_dir / f"{json_file.stem}.png"
            
            print(f"  Label: {data['label']}")
            print(f"  Taps: {len(data['tapTimestamps'])}")
            print(f"  Accelerometer samples: {len(data['accelerometerData'])}")
            print(f"  Gyroscope samples: {len(data['gyroscopeData'])}")
            
            plot_sensor_data(data, str(output_file))
            
        except Exception as e:
            print(f"  ✗ Error processing {json_file.name}: {e}")
    
    print("\n" + "=" * 60)
    print(f"✓ Batch processing complete!")
    print(f"✓ Visualizations saved to: {visualizations_dir}/")

def process_single_file(json_file, output_file=None):
    if not Path(json_file).exists():
        print(f"Error: File '{json_file}' not found")
        sys.exit(1)
    
    print(f"Loading data from {json_file}...")
    data = load_training_data(json_file)
    
    print(f"Label: {data['label']}")
    print(f"Number of taps: {len(data['tapTimestamps'])}")
    print(f"Accelerometer samples: {len(data['accelerometerData'])}")
    print(f"Gyroscope samples: {len(data['gyroscopeData'])}")
    print("\nGenerating plot...")
    
    plot_sensor_data(data, output_file)
    
    if not output_file:
        print("\nWindow opened. Close it to exit.")

def main():
    if len(sys.argv) == 1:
        batch_process()
    elif sys.argv[1] == "--help" or sys.argv[1] == "-h":
        print("Usage:")
        print("  python visualize_taps.py                        # Batch process all files")
        print("  python visualize_taps.py <json_file>            # Interactive single file")
        print("  python visualize_taps.py <json_file> <output>   # Save single file")
        print("\nBatch mode:")
        print("  Processes all JSON files in training_data/ and saves PNGs to visualizations/")
        print("\nSingle file mode:")
        print("  Specify a JSON file to visualize it individually")
    else:
        json_file = sys.argv[1]
        output_file = sys.argv[2] if len(sys.argv) > 2 else None
        process_single_file(json_file, output_file)

if __name__ == "__main__":
    main()
