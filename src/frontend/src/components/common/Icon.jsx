import PropTypes from 'prop-types';

// Import all SVG icons
import cityIcon from '../../assets/icons/city.svg';
import userIcon from '../../assets/icons/user.svg';
import emailIcon from '../../assets/icons/email.svg';
import lockIcon from '../../assets/icons/lock.svg';
import closeIcon from '../../assets/icons/close.svg';
import checkIcon from '../../assets/icons/check.svg';
import locationIcon from '../../assets/icons/location.svg';
import chartIcon from '../../assets/icons/chart.svg';
import handshakeIcon from '../../assets/icons/handshake.svg';
import wrenchIcon from '../../assets/icons/wrench.svg';
import settingsIcon from '../../assets/icons/settings.svg';
import eyeIcon from '../../assets/icons/eye.svg';
import eyeOffIcon from '../../assets/icons/eye-off.svg';
import mapIcon from '../../assets/icons/map.svg';
import waveIcon from '../../assets/icons/wave.svg';
import doorIcon from '../../assets/icons/door.svg';
import refreshIcon from '../../assets/icons/refresh.svg';
import mobileIcon from '../../assets/icons/mobile.svg';
import laptopIcon from '../../assets/icons/laptop.svg';
import desktopIcon from '../../assets/icons/desktop.svg';
import clockIcon from '../../assets/icons/clock.svg';
import prohibitedIcon from '../../assets/icons/prohibited.svg';
import magnifyingGlassIcon from '../../assets/icons/magnifying-glass.svg';
import bellIcon from '../../assets/icons/bell.svg';
import warningIcon from '../../assets/icons/warning.svg';
import scaleIcon from '../../assets/icons/scale.svg';
import timerIcon from '../../assets/icons/timer.svg';

const iconMap = {
  city: cityIcon,
  user: userIcon,
  email: emailIcon,
  lock: lockIcon,
  close: closeIcon,
  check: checkIcon,
  location: locationIcon,
  chart: chartIcon,
  handshake: handshakeIcon,
  wrench: wrenchIcon,
  settings: settingsIcon,
  eye: eyeIcon,
  'eye-off': eyeOffIcon,
  map: mapIcon,
  wave: waveIcon,
  door: doorIcon,
  refresh: refreshIcon,
  mobile: mobileIcon,
  laptop: laptopIcon,
  desktop: desktopIcon,
  clock: clockIcon,
  prohibited: prohibitedIcon,
  'magnifying-glass': magnifyingGlassIcon,
  bell: bellIcon,
  warning: warningIcon,
  scale: scaleIcon,
  timer: timerIcon,
};

/**
 * Icon component for displaying SVG icons
 * @param {string} name - Icon name from iconMap
 * @param {string} size - Icon size (small, medium, large, or custom px value)
 * @param {string} className - Additional CSS classes
 * @param {string} ariaLabel - Accessibility label
 */
function Icon({ name, size = 'medium', className = '', ariaLabel }) {
  const iconSrc = iconMap[name];

  if (!iconSrc) {
    console.warn(`Icon "${name}" not found in iconMap`);
    return null;
  }

  const sizeClass = ['small', 'medium', 'large'].includes(size) 
    ? `icon-${size}` 
    : '';

  const style = !sizeClass && size ? { width: size, height: size } : {};

  return (
    <img
      src={iconSrc}
      alt={ariaLabel || name}
      className={`icon ${sizeClass} ${className}`.trim()}
      style={style}
      aria-label={ariaLabel}
    />
  );
}

Icon.propTypes = {
  name: PropTypes.oneOf(Object.keys(iconMap)).isRequired,
  size: PropTypes.string,
  className: PropTypes.string,
  ariaLabel: PropTypes.string,
};

export default Icon;
