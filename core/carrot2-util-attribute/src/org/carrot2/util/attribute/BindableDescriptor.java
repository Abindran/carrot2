package org.carrot2.util.attribute;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.*;
import java.util.Map.Entry;

import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Provides a full description of a {@link Bindable} type, including
 * {@link AttributeDescriptor}s for all attributes defined by the type. Also provides
 * some human-readable metadata for the {@link Bindable} type itself.
 * {@link BindableDescriptor}s are immutable.
 * <p>
 * {@link BindableDescriptor}s can be obtained from
 * {@link BindableDescriptorBuilder#buildDescriptor(Object)}.
 */
public class BindableDescriptor
{
    /**
     * The supported {@link AttributeDescriptor} grouping methods.
     * 
     * @see BindableDescriptor#group(GroupingMethod)
     */
    public enum GroupingMethod
    {
        /**
         * Grouping by the {@link Class} in which the attribute is declared. Attributes
         * defined directly in the top-level {@link BindableDescriptor} will be put in
         * {@link BindableDescriptor#attributeDescriptors}.
         * <p>
         * Group key type: {@link Class}.
         */
        STRUCTURE,

        /**
         * Grouping by the {@link AttributeLevel}. Attributes without
         * {@link AttributeLevel} will be put in
         * {@link BindableDescriptor#attributeDescriptors}.
         * <p>
         * Group key type: {@link AttributeLevel}
         */
        LEVEL,

        /**
         * Grouping by the "semantic" group assigned to the attribute. Attributes with
         * undefined semantic group will be put in
         * {@link BindableDescriptor#attributeDescriptors}.
         * <p>
         * Group key type: {@link String}.
         * 
         * @see AttributeMetadata#getGroup()
         */
        GROUP,

        /**
         * No grouping, all attributes will be put in
         * {@link BindableDescriptor#attributeDescriptors}.
         * 
         * @see BindableDescriptor#flatten()
         */
        NONE
    }

    final GroupingMethod groupedBy;

    /**
     * Descriptors without any group assignment. Keys in the map correspond to attribute
     * keys as defined in {@link Attribute#key()}.
     * 
     * @see GroupingMethod
     */
    public final Map<String, AttributeDescriptor> attributeDescriptors;

    /**
     * Grouped descriptors. By default descriptors come grouped by
     * {@link GroupingMethod#STRUCTURE}, to get other groupings, use
     * {@link #group(GroupingMethod)}. The iterator returns values of this map in the
     * natural order of keys. For the exact type of the key of this map, see
     * {@link GroupingMethod}.
     */
    public final Map<Object, Map<String, AttributeDescriptor>> attributeGroups;

    /**
     * Internal descriptors for attributes defined directly in the type this
     * {@link BindableDescriptor} refers to.
     */
    final Map<String, AttributeDescriptor> attributeDescriptorsInternal;

    /**
     * Internal descriptors for other {@link Bindable} types referenced by this
     * descriptor. Keys in this map correspond to <b>fields</b> that hold the references.
     */
    final Map<Field, BindableDescriptor> bindableDescriptorsInternal;

    /**
     * Human-readable metadata about this {@link Bindable} type.
     */
    public final BindableMetadata metadata;

    /**
     * An internal constructor.
     */
    BindableDescriptor(BindableMetadata metadata,
        Map<Field, BindableDescriptor> bindableDescriptors,
        Map<String, AttributeDescriptor> attributeDescriptors)
    {
        this(metadata, bindableDescriptors, attributeDescriptors,
            GroupingMethod.STRUCTURE);
    }

    /**
     * An internal constructor.
     */
    BindableDescriptor(BindableMetadata metadata,
        Map<Field, BindableDescriptor> bindableDescriptors,
        Map<String, AttributeDescriptor> attributeDescriptors, GroupingMethod groupBy)
    {
        this.metadata = metadata;
        this.bindableDescriptorsInternal = bindableDescriptors;
        this.attributeDescriptorsInternal = attributeDescriptors;

        this.groupedBy = groupBy;

        final LinkedHashMap<String, AttributeDescriptor> newAttributeDescriptors = Maps
            .newLinkedHashMap();
        final LinkedHashMap<Object, Map<String, AttributeDescriptor>> newAttributeGroups = Maps
            .newLinkedHashMap();

        buildAttributeGroups(newAttributeDescriptors, newAttributeGroups, this, groupedBy);

        this.attributeDescriptors = Collections.unmodifiableMap(newAttributeDescriptors);
        this.attributeGroups = Collections.unmodifiableMap(newAttributeGroups);

    }

    /**
     * Preserves descriptors for which the provided <code>predicate</code> returns
     * <code>true</code>. Notice that {@link BindableDescriptor}s are immutable, so
     * the filtered descriptor set is returned rather than filtering being applied to the
     * receiver.
     * 
     * @param predicate predicate to the applied
     * @return a new {@link BindableDescriptor} with the descriptors filtered.
     */
    public BindableDescriptor only(Predicate<AttributeDescriptor> predicate)
    {
        final Map<String, AttributeDescriptor> filteredAttributeDescriptors = Maps
            .newLinkedHashMap();
        outer: for (final Map.Entry<String, AttributeDescriptor> entry : attributeDescriptorsInternal
            .entrySet())
        {
            final AttributeDescriptor descriptor = entry.getValue();
            if (!predicate.apply(descriptor))
            {
                continue outer;
            }
            filteredAttributeDescriptors.put(entry.getKey(), descriptor);
        }

        // Now recursively filter bindable descriptors
        final Map<Field, BindableDescriptor> filteredBindableDescriptors = Maps
            .newLinkedHashMap();
        for (final Map.Entry<Field, BindableDescriptor> entry : bindableDescriptorsInternal
            .entrySet())
        {
            filteredBindableDescriptors.put(entry.getKey(), entry.getValue().only(
                predicate));
        }

        return new BindableDescriptor(this.metadata, filteredBindableDescriptors,
            filteredAttributeDescriptors, this.groupedBy);
    }

    /**
     * Preserves descriptors that match all of the provided binding time and filtering
     * annotation restrictions. Notice that {@link BindableDescriptor}s are immutable, so
     * the filtered descriptor set is returned rather than filtering being applied to the
     * receiver.
     * 
     * @param annotationClasses binding time and direction annotation classes to be
     *            matched.
     * @return a new {@link BindableDescriptor} with the descriptors filtered.
     */
    @SuppressWarnings("unchecked")
    public BindableDescriptor only(final Class<? extends Annotation>... annotationClasses)
    {
        if (annotationClasses.length == 0)
        {
            return this;
        }

        return only(new Predicate<AttributeDescriptor>()
        {
            public boolean apply(AttributeDescriptor descriptor)
            {
                for (final Class<? extends Annotation> annotationClass : annotationClasses)
                {
                    if (descriptor.getAnnotation(annotationClass) == null)
                    {
                        return false;
                    }
                }

                return true;
            }
        });
    }

    /**
     * Returns a flattened structure of attribute descriptors. After flattening,
     * {@link #attributeDescriptors} contains descriptors of all attributes and
     * {@link #attributeGroups} is empty. Notice that {@link BindableDescriptor}s are
     * immutable, so the flattened descriptor set is returned rather than flattening being
     * applied to the receiver.
     * 
     * @return flattened descriptor
     */
    public BindableDescriptor flatten()
    {
        return group(GroupingMethod.NONE);
    }

    /**
     * Returns a grouped structure of attribute descriptors. Notice that
     * {@link BindableDescriptor}s are immutable, so the grouped descriptor set is
     * returned rather than grouping being applied to the receiver.
     * 
     * @param groupingMethod the grouping method to be used
     * @return grouped descriptors
     */
    public BindableDescriptor group(GroupingMethod groupingMethod)
    {
        return new BindableDescriptor(this.metadata, this.bindableDescriptorsInternal,
            this.attributeDescriptorsInternal, groupingMethod);
    }

    /**
     * Internal interface for extracting grouping key values.
     */
    static interface Grouper<T>
    {
        T getGroupingObject(Entry<Field, BindableDescriptor> bindableEntry,
            AttributeDescriptor attributeDescriptor);
    }

    @SuppressWarnings("unchecked")
    static void buildAttributeGroups(
        Map<String, AttributeDescriptor> newAttributeDescriptors,
        Map<Object, Map<String, AttributeDescriptor>> newAttributeGroups,
        BindableDescriptor bindableDescriptor, GroupingMethod groupingMethod)
    {
        final Map<Object, Map<String, AttributeDescriptor>> unsortedGroups = Maps
            .newHashMap();
        final List sortedGroupKeys;

        if (GroupingMethod.STRUCTURE.equals(groupingMethod))
        {
            addGroups(bindableDescriptor, newAttributeDescriptors, unsortedGroups,
                GROUPER_BY_STRUCTURE);

            sortedGroupKeys = Lists.newArrayList(unsortedGroups.keySet());
            Collections.sort(sortedGroupKeys, ClassComparator.INSTANCE);
        }
        else if (GroupingMethod.LEVEL.equals(groupingMethod))
        {
            addGroups(bindableDescriptor, newAttributeDescriptors, unsortedGroups,
                GROUPER_BY_LEVEL);

            sortedGroupKeys = Lists.newArrayList(unsortedGroups.keySet());
            Collections.sort(sortedGroupKeys);
        }
        else if (GroupingMethod.GROUP.equals(groupingMethod))
        {
            addGroups(bindableDescriptor, newAttributeDescriptors, unsortedGroups,
                GROUPER_BY_GROUP);

            sortedGroupKeys = Lists.newArrayList(unsortedGroups.keySet());
            Collections.sort(sortedGroupKeys);
        }
        else if (GroupingMethod.NONE.equals(groupingMethod))
        {
            addGroups(bindableDescriptor, newAttributeDescriptors, unsortedGroups,
                GROUPER_BY_NONE);
            sortedGroupKeys = Collections.EMPTY_LIST;
        }
        else
        {
            throw new IllegalArgumentException("Unknown grouping method: "
                + groupingMethod);
        }

        // Add ordered
        for (Object object : sortedGroupKeys)
        {
            newAttributeGroups.put(object, unsortedGroups.get(object));
        }
    }

    static Grouper<Class<?>> GROUPER_BY_STRUCTURE = new Grouper<Class<?>>()
    {
        public Class<?> getGroupingObject(Entry<Field, BindableDescriptor> bindableEntry,
            AttributeDescriptor attributeDescriptor)
        {
            if (bindableEntry != null)
            {
                return bindableEntry.getKey().getType();
            }
            else
            {
                return null;
            }
        }
    };

    static Grouper<AttributeLevel> GROUPER_BY_LEVEL = new Grouper<AttributeLevel>()
    {
        public AttributeLevel getGroupingObject(
            Entry<Field, BindableDescriptor> bindableEntry,
            AttributeDescriptor attributeDescriptor)
        {
            if (attributeDescriptor.metadata != null)
            {
                return attributeDescriptor.metadata.getLevel();
            }
            else
            {
                return null;
            }
        }
    };

    static Grouper<String> GROUPER_BY_GROUP = new Grouper<String>()
    {
        public String getGroupingObject(Entry<Field, BindableDescriptor> bindableEntry,
            AttributeDescriptor attributeDescriptor)
        {
            if (attributeDescriptor.metadata != null)
            {
                return attributeDescriptor.metadata.getGroup();
            }
            else
            {
                return null;
            }
        }
    };

    static Grouper<String> GROUPER_BY_NONE = new Grouper<String>()
    {
        public String getGroupingObject(Entry<Field, BindableDescriptor> bindableEntry,
            AttributeDescriptor attributeDescriptor)
        {
            return null;
        }
    };

    private static <T> void addGroups(BindableDescriptor sourceBindableDescriptor,
        Map<String, AttributeDescriptor> newAttributeDescriptors,
        Map<Object, Map<String, AttributeDescriptor>> newAttributeGroups,
        Grouper<T> grouper)
    {
        // Run through direct attribute descriptors first
        for (AttributeDescriptor attributeDescriptor1 : sourceBindableDescriptor.attributeDescriptorsInternal
            .values())
        {
            addToMaps(newAttributeDescriptors, newAttributeGroups, attributeDescriptor1,
                grouper.getGroupingObject(null, attributeDescriptor1));
        }

        // Recursively run through nested attribute descriptors
        for (Entry<Field, BindableDescriptor> entry : sourceBindableDescriptor.bindableDescriptorsInternal
            .entrySet())
        {
            for (AttributeDescriptor attributeDescriptor : entry.getValue().attributeDescriptorsInternal
                .values())
            {
                addToMaps(newAttributeDescriptors, newAttributeGroups,
                    attributeDescriptor, grouper.getGroupingObject(entry,
                        attributeDescriptor));
            }

            for (BindableDescriptor nestedBindableDescriptor : entry.getValue().bindableDescriptorsInternal
                .values())
            {
                addGroups(nestedBindableDescriptor, newAttributeDescriptors,
                    newAttributeGroups, grouper);
            }
        }
    }

    private static <T> void addToMaps(
        Map<String, AttributeDescriptor> newAttributeDescriptors,
        Map<Object, Map<String, AttributeDescriptor>> groups,
        AttributeDescriptor attributeDescriptor, final T groupingObject)
    {
        if (groupingObject == null)
        {
            newAttributeDescriptors.put(attributeDescriptor.key, attributeDescriptor);
        }
        else
        {
            Map<String, AttributeDescriptor> map = groups.get(groupingObject);
            if (map == null)
            {
                map = Maps.newLinkedHashMap();
                groups.put(groupingObject, map);
            }

            map.put(attributeDescriptor.key, attributeDescriptor);
        }
    }

    private static class ClassComparator implements Comparator<Class<?>>
    {
        public static final ClassComparator INSTANCE = new ClassComparator();

        public int compare(Class<?> o1, Class<?> o2)
        {
            return o1.getSimpleName().compareTo(o2.getSimpleName());
        }
    }
}
