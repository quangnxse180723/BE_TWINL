package com.twinl.service;

import com.twinl.dto.request.OutfitSetRequest;
import com.twinl.dto.response.OutfitSetResponse;
import java.util.List;

public interface OutfitSetService {
	List<OutfitSetResponse> getAllActive();
	List<OutfitSetResponse> getAll(); // for admin
	OutfitSetResponse getById(Long id);
	OutfitSetResponse create(OutfitSetRequest request);
	OutfitSetResponse update(Long id, OutfitSetRequest request);
	void delete(Long id);
	OutfitSetResponse toggleActive(Long id);
}
