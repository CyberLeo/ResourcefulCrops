package tehnut.resourceful.crops.core.json;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.gson.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.oredict.OreDictionary;
import tehnut.resourceful.crops.ResourcefulCrops;
import tehnut.resourceful.crops.core.data.GrowthRequirement;
import tehnut.resourceful.crops.core.data.InfoOverride;
import tehnut.resourceful.crops.core.data.Output;
import tehnut.resourceful.crops.core.data.Seed;

import java.awt.*;
import java.lang.reflect.Type;
import java.util.*;
import java.util.List;

public class Serializers {

    public static Gson withAll() {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.setPrettyPrinting();
        gsonBuilder.serializeNulls();
        gsonBuilder.disableHtmlEscaping();
        for (SerializerBase<?> serializerBase : ALL_SERIALIZERS)
            gsonBuilder.registerTypeAdapter(serializerBase.getType(), serializerBase);
        return gsonBuilder.create();
    }

    public static final SerializerBase<Seed> SEED = new SerializerBase<Seed>() {
        @Override
        public Seed deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            String name = json.getAsJsonObject().get("name").getAsString();
            int tier = json.getAsJsonObject().get("tier").getAsInt();
            int craftAmount = json.getAsJsonObject().get("craftAmount").getAsInt();
            boolean canFertilize = false;
            if (json.getAsJsonObject().has("canFertilize"))
                canFertilize = json.getAsJsonObject().get("canFertilize").getAsBoolean();
            Color color = null;
            if (json.getAsJsonObject().has("color"))
                color = Color.decode(json.getAsJsonObject().get("color").getAsString());
            List<ItemStack> inputItems = null;
            String oreName = null;
            if (json.getAsJsonObject().has("inputOre")) {
                oreName = json.getAsJsonObject().get("inputOre").getAsString();
                inputItems = OreDictionary.doesOreNameExist(oreName) ? OreDictionary.getOres(oreName) : Lists.<ItemStack>newArrayList();
            } else if (json.getAsJsonObject().has("inputItem")) {
                inputItems = Lists.newArrayList((ItemStack) context.deserialize(json.getAsJsonObject().get("inputItem"), ItemStack.class));
            } else if (json.getAsJsonObject().has("inputItems")) {
                inputItems = Lists.newArrayList((ItemStack[]) context.deserialize(json.getAsJsonObject().get("inputItems"), ItemStack[].class));
            }
            if (inputItems == null)
                throw new RuntimeException("Seed with name " + name + " does not have any valid input items.");
            Output[] outputs = context.deserialize(json.getAsJsonObject().get("outputs"), Output[].class);
            GrowthRequirement growthRequirement = context.deserialize(json.getAsJsonObject().get("growthRequirement"), GrowthRequirement.class);
            InfoOverride infoOverride = context.deserialize(json.getAsJsonObject().get("overrides"), InfoOverride.class);

            Seed seed = new Seed(name, tier, craftAmount, canFertilize, color, inputItems, outputs, growthRequirement, infoOverride);
            seed.setOreName(oreName);
            return seed;
        }

        @Override
        public JsonElement serialize(Seed src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("name", src.getName());
            jsonObject.addProperty("tier", src.getTier());
            jsonObject.addProperty("craftAmount", src.getCraftAmount());
            if (src.getColor() != null)
                jsonObject.addProperty("color", "#" + Integer.toHexString(src.getColor().getRGB()).substring(2).toUpperCase(Locale.ENGLISH));
            if (!Strings.isNullOrEmpty(src.getOreName()))
                jsonObject.addProperty("inputOre", src.getOreName());
            else if (src.getInputItems().size() == 1)
                jsonObject.add("inputItem", context.serialize(src.getInputItems().get(0)));
            else if (src.getInputItems().size() > 1)
                jsonObject.add("inputItems", context.serialize(src.getInputItems()));
            jsonObject.add("outputs", context.serialize(src.getOutputs()));
            jsonObject.add("growthRequirement", context.serialize(src.getGrowthRequirement()));
            jsonObject.add("overrides", context.serialize(src.getOverrides()));
            return jsonObject;
        }

        @Override
        public Type getType() {
            return Seed.class;
        }
    };
    public static final SerializerBase<ResourceLocation> RESOURCE_LOCATION = new SerializerBase<ResourceLocation>() {
        @Override
        public ResourceLocation deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            String domain = json.getAsJsonObject().get("domain").getAsString();
            String path = json.getAsJsonObject().get("path").getAsString();
            return new ResourceLocation(domain, path);
        }

        @Override
        public JsonElement serialize(ResourceLocation src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("domain", src.getResourceDomain());
            jsonObject.addProperty("path", src.getResourcePath());
            return jsonObject;
        }

        @Override
        public Type getType() {
            return ResourceLocation.class;
        }
    };
    public static final SerializerBase<ItemStack> ITEMSTACK = new SerializerBase<ItemStack>() {
        @Override
        public ItemStack deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            ResourceLocation registryName = context.deserialize(json.getAsJsonObject().get("id"), ResourceLocation.class);
            int amount = 1;
            if (json.getAsJsonObject().has("amount"))
                amount = json.getAsJsonObject().get("amount").getAsInt();
            int meta = 0;
            if (json.getAsJsonObject().has("meta"))
                meta = json.getAsJsonObject().get("meta").getAsInt();
            ItemStack stack = new ItemStack(ForgeRegistries.ITEMS.getValue(registryName), amount, meta);
            try {
                if (json.getAsJsonObject().has("nbt"))
                    stack.setTagCompound(JsonToNBT.getTagFromJson(json.getAsJsonObject().get("nbt").getAsString()));
            } catch (Exception e) {
                ResourcefulCrops.LOGGER.error("Error parsing NBT JSON for a stack containing {}", registryName);
                ResourcefulCrops.LOGGER.error(e.getLocalizedMessage());
            }
            return stack;
        }

        @Override
        public JsonElement serialize(ItemStack src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.add("id", context.serialize(src.getItem().getRegistryName()));
            jsonObject.addProperty("amount", src.getCount());
            jsonObject.addProperty("meta", src.getItemDamage());
            if (src.hasTagCompound())
                jsonObject.addProperty("nbt", src.getTagCompound().toString());
            return jsonObject;
        }

        @Override
        public Type getType() {
            return ItemStack.class;
        }
    };
    public static final SerializerBase<IBlockState> BLOCKSTATE = new SerializerBase<IBlockState>() {
        @Override
        public IBlockState deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            ResourceLocation block = context.deserialize(json.getAsJsonObject().get("id"), ResourceLocation.class);
            int meta = json.getAsJsonObject().get("meta").getAsInt();
            return ForgeRegistries.BLOCKS.getValue(block).getStateFromMeta(meta);
        }

        @Override
        public JsonElement serialize(IBlockState src, Type typeOfSrc, JsonSerializationContext context) {
            return super.serialize(src, typeOfSrc, context);
        }

        @Override
        public Type getType() {
            return IBlockState.class;
        }
    };
    public static final SerializerBase<Output> OUTPUT = new SerializerBase<Output>() {
        @Override
        public Output deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            ItemStack outputItem = context.deserialize(json.getAsJsonObject().get("item"), ItemStack.class);
            Output.Shape shape;
            String customFormat = null;
            if (json.getAsJsonObject().has("shape")) {
                String shapeText = json.getAsJsonObject().get("shape").getAsString();
                if (shapeText.equalsIgnoreCase("2x2"))
                    shapeText = Output.Shape.TWO_BY_TWO.name();
                else if (shapeText.equalsIgnoreCase("3x3"))
                    shapeText = Output.Shape.THREE_BY_THREE.name();
                shape = Output.Shape.valueOf(shapeText.toUpperCase(Locale.ENGLISH));

                if (shape == Output.Shape.CUSTOM) {
                    customFormat = json.getAsJsonObject().get("customFormat").getAsString();
                    if (customFormat.startsWith("#"))
                        customFormat = customFormat.substring(1, customFormat.length());
                    if (customFormat.endsWith("#"))
                        customFormat = customFormat.substring(0, customFormat.length() - 1);
                }

            } else shape = Output.Shape.DEFAULT;

            return new Output(outputItem, shape, customFormat);
        }

        @Override
        public JsonElement serialize(Output src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.add("item", context.serialize(src.getItem()));
            jsonObject.addProperty("shape", src.getShape().name());
            if (src.getShape() == Output.Shape.CUSTOM)
                jsonObject.addProperty("customFormat", src.getCustomFormat());
            return jsonObject;
        }

        @Override
        public Type getType() {
            return Output.class;
        }
    };
    public static final SerializerBase<InfoOverride.StateInfo> STATE_INFO = new SerializerBase<InfoOverride.StateInfo>() {
        @Override
        public InfoOverride.StateInfo deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            String path = InfoOverride.StateInfo.DEFAULT.getPath().toString();
            boolean shouldColor = InfoOverride.StateInfo.DEFAULT.shouldColor();

            if (json.getAsJsonObject().has("path"))
                path = json.getAsJsonObject().get("path").getAsString();
            if (json.getAsJsonObject().has("shouldColor"))
                shouldColor = json.getAsJsonObject().get("shouldColor").getAsBoolean();

            return new InfoOverride.StateInfo(path, shouldColor);
        }

        @Override
        public JsonElement serialize(InfoOverride.StateInfo src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject jsonObject = new JsonObject();
            if (src == null)
                return null;

            if (!src.getPath().equals(InfoOverride.StateInfo.DEFAULT.getPath()))
                jsonObject.addProperty("path", src.getPath().toString());
            if (src.shouldColor() != InfoOverride.StateInfo.DEFAULT.shouldColor())
                jsonObject.addProperty("shouldColor", src.shouldColor());

            return jsonObject;
        }

        @Override
        public Type getType() {
            return InfoOverride.StateInfo.class;
        }
    };
    public static final SerializerBase<InfoOverride.ModelInfo> MODEL_INFO = new SerializerBase<InfoOverride.ModelInfo>() {

        @Override
        public InfoOverride.ModelInfo deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            String path = InfoOverride.ModelInfo.DEFAULT.getPath().toString();
            String variant = InfoOverride.ModelInfo.DEFAULT.getVariant();
            boolean shouldColor = InfoOverride.ModelInfo.DEFAULT.shouldColor();

            if (json.getAsJsonObject().has("path"))
                path = json.getAsJsonObject().get("path").getAsString();
            if (json.getAsJsonObject().has("variant"))
                variant = json.getAsJsonObject().get("variant").getAsString();
            if (json.getAsJsonObject().has("shouldColor"))
                shouldColor = json.getAsJsonObject().get("shouldColor").getAsBoolean();

            return new InfoOverride.ModelInfo(path, variant, shouldColor);
        }

        @Override
        public JsonElement serialize(InfoOverride.ModelInfo src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject jsonObject = new JsonObject();
            if (src == null)
                return null;

            if (!src.getPath().equals(InfoOverride.ModelInfo.DEFAULT.getPath()))
                jsonObject.addProperty("path", src.getPath().toString());
            if (!src.getVariant().equals(InfoOverride.ModelInfo.DEFAULT.getVariant()))
                jsonObject.addProperty("variant", src.getVariant());
            if (src.shouldColor() != InfoOverride.ModelInfo.DEFAULT.shouldColor())
                jsonObject.addProperty("shouldColor", src.shouldColor());

            return jsonObject;
        }

        @Override
        public Type getType() {
            return InfoOverride.ModelInfo.class;
        }
    };

    private static final SerializerBase<?>[] ALL_SERIALIZERS = new SerializerBase[] {
            SEED,
            RESOURCE_LOCATION,
            ITEMSTACK,
            BLOCKSTATE,
            OUTPUT,
            STATE_INFO,
            MODEL_INFO
    };
}
