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
    mag_data = data.get('magnetometerData', [])
    linear_accel_data = data.get('linearAccelerationData', [])
    gravity_data = data.get('gravityData', [])
    rotation_vector_data = data.get('rotationVectorData', [])
    
    accel_times = [point['timestampMs'] for point in accel_data]
    accel_x = [point['x'] for point in accel_data]
    accel_y = [point['y'] for point in accel_data]
    accel_z = [point['z'] for point in accel_data]
    
    gyro_times = [point['timestampMs'] for point in gyro_data]
    gyro_x = [point['x'] for point in gyro_data]
    gyro_y = [point['y'] for point in gyro_data]
    gyro_z = [point['z'] for point in gyro_data]

    start_time = accel_times[0]
    accel_times_rel = [(t - start_time) / 1000.0 for t in accel_times]
    gyro_times_rel = [(t - start_time) / 1000.0 for t in gyro_times]
    tap_times_rel = [(t - start_time) / 1000.0 for t in tap_timestamps]

    num_plots = 2
    if mag_data:
        num_plots += 1
    if linear_accel_data:
        num_plots += 1
    if gravity_data:
        num_plots += 1
    if rotation_vector_data:
        num_plots += 1

    fig, axes = plt.subplots(num_plots, 1, figsize=(14, 5 * num_plots))
    if num_plots == 1:
        axes = [axes]

    fig.suptitle(f'Sensor Data - {label}\n({len(tap_timestamps)} taps)', fontsize=16, fontweight='bold')

    plot_idx = 0

    ax1 = axes[plot_idx]
    plot_idx += 1
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

    ax2 = axes[plot_idx]
    plot_idx += 1
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

    if mag_data:
        mag_times = [point['timestampMs'] for point in mag_data]
        mag_x = [point['x'] for point in mag_data]
        mag_y = [point['y'] for point in mag_data]
        mag_z = [point['z'] for point in mag_data]
        mag_times_rel = [(t - start_time) / 1000.0 for t in mag_times]

        ax3 = axes[plot_idx]
        plot_idx += 1
        ax3.plot(mag_times_rel, mag_x, label='X', linewidth=1, alpha=0.8)
        ax3.plot(mag_times_rel, mag_y, label='Y', linewidth=1, alpha=0.8)
        ax3.plot(mag_times_rel, mag_z, label='Z', linewidth=1, alpha=0.8)

        for i, tap_time in enumerate(tap_times_rel):
            ax3.axvline(x=tap_time, color='red', linestyle='--', linewidth=2, alpha=0.7,
                        label='Tap' if i == 0 else '')
            ax3.text(tap_time, ax3.get_ylim()[1], f'{i + 1}', ha='center', va='bottom', fontsize=10,
                     color='red', fontweight='bold')

        ax3.set_xlabel('Time (seconds)', fontsize=12)
        ax3.set_ylabel('Magnetic Field (μT)', fontsize=12)
        ax3.set_title('Magnetometer Data', fontsize=14, fontweight='bold')
        ax3.legend(loc='upper right')
        ax3.grid(True, alpha=0.3)

    if linear_accel_data:
        linear_times = [point['timestampMs'] for point in linear_accel_data]
        linear_x = [point['x'] for point in linear_accel_data]
        linear_y = [point['y'] for point in linear_accel_data]
        linear_z = [point['z'] for point in linear_accel_data]
        linear_times_rel = [(t - start_time) / 1000.0 for t in linear_times]

        ax4 = axes[plot_idx]
        plot_idx += 1
        ax4.plot(linear_times_rel, linear_x, label='X', linewidth=1, alpha=0.8)
        ax4.plot(linear_times_rel, linear_y, label='Y', linewidth=1, alpha=0.8)
        ax4.plot(linear_times_rel, linear_z, label='Z', linewidth=1, alpha=0.8)

        for i, tap_time in enumerate(tap_times_rel):
            ax4.axvline(x=tap_time, color='red', linestyle='--', linewidth=2, alpha=0.7,
                        label='Tap' if i == 0 else '')
            ax4.text(tap_time, ax4.get_ylim()[1], f'{i + 1}', ha='center', va='bottom', fontsize=10,
                     color='red', fontweight='bold')

        ax4.set_xlabel('Time (seconds)', fontsize=12)
        ax4.set_ylabel('Acceleration (m/s²)', fontsize=12)
        ax4.set_title('Linear Acceleration Data', fontsize=14, fontweight='bold')
        ax4.legend(loc='upper right')
        ax4.grid(True, alpha=0.3)

    if gravity_data:
        gravity_times = [point['timestampMs'] for point in gravity_data]
        gravity_x = [point['x'] for point in gravity_data]
        gravity_y = [point['y'] for point in gravity_data]
        gravity_z = [point['z'] for point in gravity_data]
        gravity_times_rel = [(t - start_time) / 1000.0 for t in gravity_times]

        ax5 = axes[plot_idx]
        plot_idx += 1
        ax5.plot(gravity_times_rel, gravity_x, label='X', linewidth=1, alpha=0.8)
        ax5.plot(gravity_times_rel, gravity_y, label='Y', linewidth=1, alpha=0.8)
        ax5.plot(gravity_times_rel, gravity_z, label='Z', linewidth=1, alpha=0.8)

        for i, tap_time in enumerate(tap_times_rel):
            ax5.axvline(x=tap_time, color='red', linestyle='--', linewidth=2, alpha=0.7,
                        label='Tap' if i == 0 else '')
            ax5.text(tap_time, ax5.get_ylim()[1], f'{i + 1}', ha='center', va='bottom', fontsize=10,
                     color='red', fontweight='bold')

        ax5.set_xlabel('Time (seconds)', fontsize=12)
        ax5.set_ylabel('Gravity (m/s²)', fontsize=12)
        ax5.set_title('Gravity Data', fontsize=14, fontweight='bold')
        ax5.legend(loc='upper right')
        ax5.grid(True, alpha=0.3)

    if rotation_vector_data:
        rotation_vector_times = [point['timestampMs'] for point in rotation_vector_data]
        rotation_vector_x = [point['x'] for point in rotation_vector_data]
        rotation_vector_y = [point['y'] for point in rotation_vector_data]
        rotation_vector_z = [point['z'] for point in rotation_vector_data]
        rotation_vector_times_rel = [(t - start_time) / 1000.0 for t in rotation_vector_times]

        ax6 = axes[plot_idx]
        ax6.plot(rotation_vector_times_rel, rotation_vector_x, label='X', linewidth=1, alpha=0.8)
        ax6.plot(rotation_vector_times_rel, rotation_vector_y, label='Y', linewidth=1, alpha=0.8)
        ax6.plot(rotation_vector_times_rel, rotation_vector_z, label='Z', linewidth=1, alpha=0.8)

        for i, tap_time in enumerate(tap_times_rel):
            ax6.axvline(x=tap_time, color='red', linestyle='--', linewidth=2, alpha=0.7,
                        label='Tap' if i == 0 else '')
            ax6.text(tap_time, ax6.get_ylim()[1], f'{i + 1}', ha='center', va='bottom', fontsize=10,
                     color='red', fontweight='bold')

        ax6.set_xlabel('Time (seconds)', fontsize=12)
        ax6.set_ylabel('Rotation Vector', fontsize=12)
        ax6.set_title('Rotation Vector Data', fontsize=14, fontweight='bold')
        ax6.legend(loc='upper right')
        ax6.grid(True, alpha=0.3)

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
            print(f"  Magnetometer samples: {len(data.get('magnetometerData', []))}")
            print(f"  Linear Acceleration samples: {len(data.get('linearAccelerationData', []))}")
            print(f"  Gravity samples: {len(data.get('gravityData', []))}")
            print(f"  Rotation Vector samples: {len(data.get('rotationVectorData', []))}")
            
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
    print(f"Magnetometer samples: {len(data.get('magnetometerData', []))}")
    print(f"Linear Acceleration samples: {len(data.get('linearAccelerationData', []))}")
    print(f"Gravity samples: {len(data.get('gravityData', []))}")
    print(f"Rotation Vector samples: {len(data.get('rotationVectorData', []))}")
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
