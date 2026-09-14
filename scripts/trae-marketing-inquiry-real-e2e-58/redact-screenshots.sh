#!/bin/bash
# Redact phone numbers in screenshots using ImageMagick
cd /tmp/shots58

for f in 01-submit-success.jpg 02-lookup-success.jpg 03-wrong-phone-empty.jpg 04-invalid-inquiry-empty.jpg 05-system-error.jpg; do
  if [ ! -f "$f" ]; then echo "  SKIP $f (not found)"; continue; fi
  W=$(identify -format '%w' "$f" 2>/dev/null)
  H=$(identify -format '%h' "$f" 2>/dev/null)
  echo "Processing $f (${W}x${H})..."

  case "$f" in
    01-submit-success.jpg)
      Y1=$(echo "$H*55/100" | bc); Y2=$(echo "$H*65/100" | bc)
      convert "$f" -fill black -draw "rectangle 0,${Y1} ${W},${Y2}" "redacted-$f"
      ;;
    02-lookup-success.jpg)
      Y1=$(echo "$H*25/100" | bc); Y2=$(echo "$H*45/100" | bc)
      convert "$f" -fill black -draw "rectangle 0,${Y1} ${W},${Y2}" "redacted-$f"
      ;;
    03-wrong-phone-empty.jpg)
      Y1=$(echo "$H*25/100" | bc); Y2=$(echo "$H*45/100" | bc)
      convert "$f" -fill black -draw "rectangle 0,${Y1} ${W},${Y2}" "redacted-$f"
      ;;
    04-invalid-inquiry-empty.jpg)
      Y1=$(echo "$H*25/100" | bc); Y2=$(echo "$H*45/100" | bc)
      convert "$f" -fill black -draw "rectangle 0,${Y1} ${W},${Y2}" "redacted-$f"
      ;;
    05-system-error.jpg)
      Y1=$(echo "$H*30/100" | bc); Y2=$(echo "$H*50/100" | bc)
      convert "$f" -fill black -draw "rectangle 0,${Y1} ${W},${Y2}" "redacted-$f"
      ;;
  esac
  echo "  done -> redacted-$f"
done
echo "ALL_REDACTED"
