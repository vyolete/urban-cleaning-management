#!/bin/bash

# Install Load Testing Tools
# This script installs the necessary tools for load testing

echo "========================================="
echo "Load Testing Tools Installation"
echo "========================================="
echo ""

# Detect OS
OS="$(uname -s)"
echo "Detected OS: $OS"
echo ""

# Function to check if command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Install wrk (HTTP benchmarking tool)
install_wrk() {
    echo "Installing wrk..."
    
    case "$OS" in
        Darwin*)
            # macOS
            if command_exists brew; then
                brew install wrk
            else
                echo "Homebrew not found. Please install Homebrew first:"
                echo "  /bin/bash -c \"\$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)\""
                return 1
            fi
            ;;
        Linux*)
            # Linux
            if command_exists apt-get; then
                sudo apt-get update
                sudo apt-get install -y wrk
            elif command_exists yum; then
                sudo yum install -y wrk
            else
                echo "Package manager not supported. Please install wrk manually."
                return 1
            fi
            ;;
        *)
            echo "OS not supported for automatic installation"
            return 1
            ;;
    esac
}

# Install Apache Bench (comes with Apache)
install_ab() {
    echo "Installing Apache Bench (ab)..."
    
    case "$OS" in
        Darwin*)
            # macOS - ab comes pre-installed
            echo "Apache Bench is pre-installed on macOS"
            ;;
        Linux*)
            # Linux
            if command_exists apt-get; then
                sudo apt-get update
                sudo apt-get install -y apache2-utils
            elif command_exists yum; then
                sudo yum install -y httpd-tools
            else
                echo "Package manager not supported"
                return 1
            fi
            ;;
        *)
            echo "OS not supported"
            return 1
            ;;
    esac
}

# Install JMeter
install_jmeter() {
    echo "Installing Apache JMeter..."
    
    case "$OS" in
        Darwin*)
            # macOS
            if command_exists brew; then
                brew install jmeter
            else
                echo "Homebrew not found"
                return 1
            fi
            ;;
        Linux*)
            # Linux - download and extract
            JMETER_VERSION="5.6.3"
            JMETER_URL="https://dlcdn.apache.org//jmeter/binaries/apache-jmeter-${JMETER_VERSION}.tgz"
            
            echo "Downloading JMeter ${JMETER_VERSION}..."
            wget "$JMETER_URL" -O /tmp/jmeter.tgz
            
            echo "Extracting JMeter..."
            sudo tar -xzf /tmp/jmeter.tgz -C /opt/
            sudo ln -sf /opt/apache-jmeter-${JMETER_VERSION}/bin/jmeter /usr/local/bin/jmeter
            
            rm /tmp/jmeter.tgz
            echo "JMeter installed to /opt/apache-jmeter-${JMETER_VERSION}"
            ;;
        *)
            echo "OS not supported"
            return 1
            ;;
    esac
}

# Check and install tools
echo "Checking installed tools..."
echo ""

# Check wrk
if command_exists wrk; then
    echo "✓ wrk is already installed"
    wrk --version
else
    echo "✗ wrk is not installed"
    read -p "Install wrk? (y/n) " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        install_wrk
    fi
fi
echo ""

# Check ab
if command_exists ab; then
    echo "✓ Apache Bench (ab) is already installed"
    ab -V | head -n 1
else
    echo "✗ Apache Bench (ab) is not installed"
    read -p "Install Apache Bench? (y/n) " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        install_ab
    fi
fi
echo ""

# Check JMeter
if command_exists jmeter; then
    echo "✓ Apache JMeter is already installed"
    jmeter --version | head -n 1
else
    echo "✗ Apache JMeter is not installed"
    read -p "Install Apache JMeter? (y/n) " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        install_jmeter
    fi
fi
echo ""

# Check other dependencies
echo "Checking other dependencies..."
echo ""

if command_exists curl; then
    echo "✓ curl is installed"
else
    echo "✗ curl is not installed (required)"
fi

if command_exists jq; then
    echo "✓ jq is installed"
else
    echo "✗ jq is not installed (optional, for JSON parsing)"
    echo "  Install with: brew install jq (macOS) or apt-get install jq (Linux)"
fi

if command_exists bc; then
    echo "✓ bc is installed"
else
    echo "✗ bc is not installed (required for calculations)"
fi

echo ""
echo "========================================="
echo "Installation Complete!"
echo "========================================="
echo ""
echo "You can now run load tests using:"
echo "  ./run-comprehensive-load-test.sh"
echo ""
