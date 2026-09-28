# SDK Manager

A powerful Android system-tuning and device-management application designed to provide a modern interface for monitoring and controlling supported kernel and system performance features.

> **Root access is required for features that modify kernel/system parameters.**
> Feature availability depends on the device, kernel, Android version, and available sysfs interfaces.

## ✨ Features

### 🏠 Home Dashboard

The Home page provides a quick overview of the device's current system state.

#### CPU
- Displays CPU information and current CPU frequencies.
- Shows the maximum CPU frequency for the available CPU clusters.
- Displays the current CPU governor for each CPU cluster.
- Provides a quick overview of the current CPU performance configuration.

#### RAM
- Displays current RAM usage and memory information.
- Provides a quick overview of used and available memory.

#### ZRAM
- Displays ZRAM information and current ZRAM configuration.
- Helps monitor compressed memory usage.

#### Current Profile
The Home page displays the currently applied performance profile.

When no profile has been applied:

**No profile applied**

A **Go to Backup →** button opens the Backup page.

When a profile is active, the current profile name is displayed together with a **Choose** button, allowing the user to select another profile.

---

# ⚡ Profiles

SDK Manager supports configurable performance profiles that allow users to quickly switch between different system configurations.

## Built-in Profiles

The application includes five built-in profiles:

- 🎮 **Gaming**
- 🚀 **Performance**
- ⚖️ **Balance**
- 🔋 **Battery Saver**
- 🛡️ **Ultra Power Saver**

Built-in profiles are provided as predefined JSON configurations.

### Built-in Profile Protection

Built-in profiles are templates provided by the application.

Users can:

- View built-in profiles
- Apply built-in profiles
- Switch between built-in profiles

Users cannot:

- Delete built-in profiles
- Export built-in profiles

This prevents accidental removal or modification of predefined templates.

---

## 👤 Custom Profiles

Users can create their own profiles just like before.

Custom profiles allow users to save their preferred system settings and restore them later.

Custom profiles can be used alongside the built-in profiles.

---

## 📦 Profile Application

When a profile is applied, SDK Manager processes the settings contained in that profile.

After applying a profile, the **Apply** button becomes disabled/gray.

If any setting fails during application, the Apply button is also disabled/gray so the profile cannot be repeatedly applied through the same active Apply state.

---

# 💾 Backup / Profiles Page

The Backup page acts as the central location for profile management.

It contains:

- Built-in profiles
- User-created profiles
- Profile application controls
- Profile information
- Profile configuration data

Built-in profiles are stored as JSON templates so their settings can be consistently applied.

---

# 🧠 Memory Management

SDK Manager provides memory-related controls for supported devices.

### Swapness

The memory **Swapness** control allows users to configure the kernel's memory swapping behavior.

The slider range is:

**0 → 100**

### ZRAM

Supported devices can expose ZRAM-related information and controls through the application.

ZRAM can help Android use compressed memory as an additional memory resource.

---

# 📋 Log

The Log page provides quick access to supported logging/debugging functions.

The interface contains seven log-related buttons arranged in a compact layout for easy access.

Buttons **5 and 6** follow the same sizing and positioning style as the existing buttons to keep the interface visually consistent.

---

# 🎨 User Interface

SDK Manager uses a modern card-based interface designed for quick access to important system information.

The UI includes:

- Dashboard cards
- CPU information
- RAM information
- ZRAM information
- Current profile information
- Profile cards
- Built-in profile artwork
- Log controls
- Memory controls
- Apply and selection actions

Built-in profiles include dedicated visual artwork displayed beside the profile name.

---

# 🔧 System Control

Depending on device and kernel support, SDK Manager can interact with system/kernel interfaces to modify supported parameters.

Possible controls depend on what the device exposes through interfaces such as:

- `/sys`
- CPU frequency interfaces
- CPU governor interfaces
- Memory interfaces
- ZRAM interfaces
- Other supported kernel controls

Because Android kernels differ between devices, not every feature will be available on every device.

---

# 🔐 Root Requirements

Many SDK Manager features require elevated privileges.

For full functionality, the device may require:

- Root access
- A compatible kernel
- Required sysfs interfaces
- Appropriate permissions

If a particular kernel parameter does not exist on a device, SDK Manager cannot safely control that parameter.

---

# 📱 Device Compatibility

SDK Manager is designed for Android devices with exposed kernel/system controls.

Compatibility depends on:

- Android version
- Kernel version
- Device manufacturer
- CPU architecture
- Kernel configuration
- Available sysfs nodes
- Root implementation
- SELinux configuration
- Vendor-specific modifications

A feature working on one device does not necessarily mean the same feature will work on another device.

---

# ⚠️ Important Warning

Changing CPU, memory, governor, ZRAM, or other kernel parameters can affect:

- Performance
- Battery life
- Temperature
- Stability
- System responsiveness

Some settings may be unsafe or unsupported on particular devices.

**Use system-tuning features at your own risk.**

---

# 🛠️ Development

SDK Manager is an Android project intended for developers and advanced Android users interested in system and kernel management.

The project can be opened and built using Android Studio or a compatible Gradle environment.

Before building, make sure your development environment has the required:

- Android SDK
- Android build tools
- JDK
- Gradle dependencies

---

# 📂 Profile Format

Profiles use JSON configuration data.

A profile can contain the values required by SDK Manager to configure supported system parameters.

Example:

```json
{
  "name": "Gaming",
  "settings": {
    "example_setting": "value"
  }
}
```

The exact available settings depend on the implementation and device.

---

# 🚀 Project Goals

SDK Manager aims to provide a simple way to manage advanced Android system settings without requiring users to manually edit sysfs files or execute multiple shell commands.

Main goals:

- Simple system tuning
- Fast profile switching
- Clear system monitoring
- Built-in performance presets
- Custom user profiles
- Memory management
- CPU monitoring
- Kernel/logging controls
- Modern and easy-to-use UI

---

# 📌 Built-in Profiles

| Profile | Purpose |
|---|---|
| 🎮 Gaming | Gaming-focused configuration |
| 🚀 Performance | Performance-focused configuration |
| ⚖️ Balance | Balanced performance and battery usage |
| 🔋 Battery Saver | Reduced power consumption |
| 🛡️ Ultra Power Saver | Maximum power-saving configuration |

---

# 🤝 Contributing

Contributions, bug reports, feature suggestions, and improvements are welcome.

When reporting an issue, include:

- Device model
- Android version
- Kernel version
- Root solution
- Relevant logs
- Description of the problem
- Steps to reproduce the issue

---

# ⚖️ Disclaimer

SDK Manager is provided for educational and advanced system-management purposes.

Modifying kernel or system parameters can cause instability, excessive heat, battery drain, crashes, or other unexpected behavior.

Always understand a setting before changing it and keep a recovery method available.

---

## ⭐ Project

If you find SDK Manager useful, consider supporting the project by:

- ⭐ Starring the repository
- 🐛 Reporting bugs
- 💡 Suggesting improvements
- 🔧 Contributing code
- 📖 Improving documentation

**SDK Manager — Simple interface. Powerful system control.**
