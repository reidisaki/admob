CONFIG=Debug
OUTPUT_DIST=build
MODULE=OverlayRendererDemo
TARGET_FRAMEWORK=${OUTPUT_DIST}/${MODULE}.framework
BINARY=lib${MODULE}.a

all: clean framework

clean:
	rm -rf build/ 

framework:
	xcodebuild -configuration ${CONFIG} -sdk iphoneos || exit 1
	xcodebuild -configuration ${CONFIG} -sdk iphonesimulator || exit 2
	mkdir -p ${TARGET_FRAMEWORK}/Headers ${TARGET_FRAMEWORK}/Resources ${TARGET_FRAMEWORK}/Libraries || exit 3
	cp Classes/${MODULE}.h ${TARGET_FRAMEWORK}/Headers/ || exit 4
	lipo -create "build/${CONFIG}-iphoneos/${BINARY}" "build/${CONFIG}-iphonesimulator/${BINARY}" -output "${TARGET_FRAMEWORK}/${BINARY}" || exit 5
	mv ${TARGET_FRAMEWORK}/${BINARY} ${TARGET_FRAMEWORK}/Libraries || exit 6
	cd ${TARGET_FRAMEWORK} && ln -sv Libraries/${BINARY} ${MODULE}
	echo Created ${TARGET_FRAMEWORK}
