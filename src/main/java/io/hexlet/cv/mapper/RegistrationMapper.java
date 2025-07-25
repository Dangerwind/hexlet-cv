package io.hexlet.cv.mapper;

import io.hexlet.cv.dto.registration.RegInputDTO;
import io.hexlet.cv.dto.registration.RegOutputDTO;
import io.hexlet.cv.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

@Mapper(uses = {
        JsonNullableMapper.class}, nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public abstract class RegistrationMapper {
    // @Autowired
    // private BCryptPasswordEncoder encoder;

    // @Mapping(target = "encryptedPassword", source = "password")
    public abstract User map(RegInputDTO dto);

    public abstract RegOutputDTO map(User user);

    // @BeforeMapping
    // public void encryptPassword(RegInputDTO data) {
    // var password = data.getPassword();
    // data.setPassword(encoder.encode(password));
    // }
}
